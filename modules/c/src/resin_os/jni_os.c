/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
 *
 * Caucho Technology permits modification and use of this file in
 * source and binary form ("the Software") subject to the Caucho
 * Developer Source License 1.1 ("the License") which accompanies
 * this file.  The License is also available at
 *   http://www.caucho.com/download/cdsl1-1.xtp
 *
 * In addition to the terms of the License, the following conditions
 * must be met:
 *
 * 1. Each copy or derived work of the Software must preserve the copyright
 *    notice and this notice unmodified.
 *
 * 2. Each copy of the Software in source or binary form must include 
 *    an unmodified copy of the License in a plain ASCII text file named
 *    LICENSE.
 *
 * 3. Caucho reserves all rights to its names, trademarks and logos.
 *    In particular, the names "Resin" and "Caucho" are trademarks of
 *    Caucho and may not be used to endorse products derived from
 *    this software.  "Resin" and "Caucho" may not appear in the names
 *    of products derived from this software.
 *
 * This Software is provided "AS IS," without a warranty of any kind. 
 * ALL EXPRESS OR IMPLIED REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED.
 *
 * CAUCHO TECHNOLOGY AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE OR ANY THIRD PARTY AS A RESULT OF USING OR
 * DISTRIBUTING SOFTWARE. IN NO EVENT WILL CAUCHO OR ITS LICENSORS BE LIABLE
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE SOFTWARE, EVEN IF HE HAS BEEN ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGES.      
 *
 * @author Scott Ferguson
 */

#ifdef WIN32
#include <windows.h>
#else
#include <sys/types.h>
#include <sys/socket.h>
#include <unistd.h>
#include <sys/time.h>
#include <pwd.h>
#include <syslog.h>
#include <netdb.h>
#endif

#include <sys/stat.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
#include <signal.h>
#include <errno.h>
/* probably system-dependent */
#include <jni.h>

#ifdef HAS_JVMTI
#include <jvmti.h>
#else
#include <jvmdi.h>
#endif


static int
set_byte_array_region(JNIEnv *env, jbyteArray buf, jint offset, jint sublen,
		      char *buffer)
{
  jbyte *cBuf;
  
  /* (*env)->SetByteArrayRegion(env, buf, offset, sublen, buffer); */
  
  cBuf = (*env)->GetPrimitiveArrayCritical(env, buf, 0);

  if (cBuf) {
    memcpy(cBuf + offset, buffer, sublen);

    (*env)->ReleasePrimitiveArrayCritical(env, buf, cBuf, 0);

    return 1;
  }
  
  return 0;
}

static int
get_byte_array_region(JNIEnv *env, jbyteArray buf, jint offset, jint sublen,
		      char *buffer)
{
  jbyte *cBuf = (*env)->GetPrimitiveArrayCritical(env, buf, 0);

  if (cBuf) {
    memcpy(buffer, cBuf + offset, sublen);

    (*env)->ReleasePrimitiveArrayCritical(env, buf, cBuf, 0);

    return 1;
  }
  
  return 0;
}

void
resin_printf_exception(JNIEnv *env, const char *cl, const char *fmt, ...)
{
  char buf[8192];
  va_list list;
  jclass clazz;

  va_start(list, fmt);

  vsprintf(buf, fmt, list);

  va_end(list);

  if (env && ! (*env)->ExceptionOccurred(env)) {
    clazz = (*env)->FindClass(env, cl);

    if (clazz) {
      (*env)->ThrowNew(env, clazz, buf);
      return;
    }
  }

  fprintf(stderr, "%s\n", buf);
}

#ifdef HAS_JVMTI

jboolean
Java_com_caucho_loader_ClassEntry_canReloadNative(JNIEnv *env,
					          jobject obj)
{
  JavaVM *jvm = 0;
  jvmtiEnv *jvmti = 0;
  jvmtiCapabilities capabilities;
  jvmtiCapabilities set_capabilities;
  int res;

  res = (*env)->GetJavaVM(env, &jvm);
  if (res < 0)
    return 0;
  
  res = (*jvm)->GetEnv(jvm, (void **)&jvmti, JVMTI_VERSION_1_0);

  if (res < 0 || jvmti == 0)
    return 0;

  memset(&set_capabilities, 0, sizeof(capabilities));
  set_capabilities.can_redefine_classes = 1;
  (*jvmti)->AddCapabilities(jvmti, &set_capabilities);
  
  (*jvmti)->GetCapabilities(jvmti, &capabilities);

  (*jvmti)->RelinquishCapabilities(jvmti, &set_capabilities);
  
  return capabilities.can_redefine_classes;
}

jint
Java_com_caucho_loader_ClassEntry_reloadNative(JNIEnv *env,
					       jobject obj,
					       jclass cl,
					       jbyteArray buf,
					       jint offset,
					       jint length)
{
  JavaVM *jvm = 0;
  jvmtiEnv *jvmti = 0;
  int res;
  jvmtiClassDefinition defs[1];
  char *class_def;
  jvmtiCapabilities capabilities;

  if (cl == 0 || buf == 0)
    return 0;

  res = (*env)->GetJavaVM(env, &jvm);
  if (res < 0)
    return -1;
  
  res = (*jvm)->GetEnv(jvm, (void **)&jvmti, JVMTI_VERSION_1_0);

  if (res < 0 || jvmti == 0)
    return 0;

  memset(&capabilities, 0, sizeof(capabilities));
  capabilities.can_redefine_classes = 1;
  (*jvmti)->AddCapabilities(jvmti, &capabilities);
  
  defs[0].klass = cl;
  defs[0].class_byte_count = length;
  class_def = (*env)->GetByteArrayElements(env, buf, 0);
  defs[0].class_bytes = class_def + offset;

  if (defs[0].class_bytes) {
    res = (*jvmti)->RedefineClasses(jvmti, 1, defs);

    (*env)->ReleaseByteArrayElements(env, buf, class_def, 0);
  }
  
  (*jvmti)->RelinquishCapabilities(jvmti, &capabilities);
  
  return res;
}

#else

jboolean
Java_com_caucho_loader_ClassEntry_canReloadNative(JNIEnv *env,
					          jobject obj)
{
  JavaVM *jvm = 0;
  JVMDI_Interface_1 *jvmdi = 0;
  JVMDI_capabilities capabilities;
  int res;

  res = (*env)->GetJavaVM(env, &jvm);
  if (res < 0) {
    return 0;
  }
  
  res = (*jvm)->GetEnv(jvm, (void **)&jvmdi, JVMDI_VERSION_1);

  if (res < 0 || jvmdi == 0)
    return 0;

  (jvmdi)->GetCapabilities(&capabilities);

  return capabilities.can_redefine_classes;
}

jint
Java_com_caucho_loader_ClassEntry_reloadNative(JNIEnv *env,
					       jobject obj,
					       jclass cl,
					       jbyteArray buf,
					       jint offset,
					       jint length)
{
  JavaVM *jvm = 0;
  JVMDI_Interface_1 *jvmdi = 0;
  int res;
  JVMDI_class_definition defs[1];
  char *class_def;

  if (cl == 0 || buf == 0)
    return 0;

  res = (*env)->GetJavaVM(env, &jvm);
  if (res < 0)
    return -1;
  
  res = (*jvm)->GetEnv(jvm, (void **)&jvmdi, JVMDI_VERSION_1);
  
  if (res < 0 || jvmdi == 0)
    return -1;

  defs[0].clazz = cl;
  defs[0].class_byte_count = length;
  class_def = (*env)->GetByteArrayElements(env, buf, 0);
  defs[0].class_bytes = class_def + offset;

  if (defs[0].class_bytes) {
    res = jvmdi->RedefineClasses(1, defs);

    (*env)->ReleaseByteArrayElements(env, buf, class_def, 0);
  }
  
  return res;
}
						 
#endif

static char *
get_utf8(JNIEnv *env, jstring jaddr, char *buf, int buflen)
{
  const char *temp_string = 0;

  temp_string = (*env)->GetStringUTFChars(env, jaddr, 0);
  
  if (temp_string) {
    strncpy(buf, temp_string, buflen);
    buf[buflen - 1] = 0;
  
    (*env)->ReleaseStringUTFChars(env, jaddr, temp_string);
  }

  return buf;
}

jint
Java_com_caucho_server_boot_ResinBoot_execDaemon(JNIEnv *env,
						 jobject obj,
						 jobjectArray j_argv,
						 jobjectArray j_envp,
						 jstring j_pwd)
{
  char **argv;
  char **envp;
  char *pwd;
  int len;
  int i;
  
  if (! j_argv)
    resin_printf_exception(env, "java/lang/NullPointerException", "argv");
  if (! j_envp)
    resin_printf_exception(env, "java/lang/NullPointerException", "argv");
  if (! j_pwd)
    resin_printf_exception(env, "java/lang/NullPointerException", "pwd");

#ifdef WIN32
  resin_printf_exception(env, "java/lang/UnsupportedOperationException", "win32");
#else
  len = (*env)->GetArrayLength(env, j_argv);
  argv = malloc((len + 1) * sizeof(char*));
  argv[len] = 0;
  
  for (i = 0; i < len; i++) {
    jstring j_string;

    j_string = (*env)->GetObjectArrayElement(env, j_argv, i);

    if (j_string) {
      int strlen = (*env)->GetStringUTFLength(env, j_string);
      
      argv[i] = (char *) malloc(strlen + 1);
    
      argv[i] = get_utf8(env, j_string, argv[i], strlen + 1);
    }
  }

  len = (*env)->GetArrayLength(env, j_envp);
  envp = malloc((len + 1) * sizeof(char*));
  envp[len] = 0;
  
  for (i = 0; i < len; i++) {
    jstring j_string;

    j_string = (*env)->GetObjectArrayElement(env, j_envp, i);

    if (j_string) {
      int strlen = (*env)->GetStringUTFLength(env, j_string);
      
      envp[i] = (char *) malloc(strlen + 1);
    
      envp[i] = get_utf8(env, j_string, envp[i], strlen + 1);
    }
  }

  {
    int strlen = (*env)->GetStringUTFLength(env, j_pwd);
    char *pwd;

    pwd = (char *) malloc(strlen + 1);
    pwd = get_utf8(env, j_pwd, pwd, strlen + 1);

    chdir(pwd);
  }

  if (fork())
    return 1;
  
  if (fork())
    exit(0);

#ifndef WIN32
  setsid();
#endif /* WIN32 */

  execve(argv[0], argv, envp);

  fprintf(stderr, "exec failed %s -> %d\n", argv[0], errno);
  exit(1);
#endif  
  return -1;
}
