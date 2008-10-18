#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <dirent.h>
#include <pwd.h>

static void
chown_rec(char *path, int uid, int gid)
{
  DIR *dir;
  struct dirent *dirent;
  char buf[2048];
  
  if (chown(path, uid, gid) < 0) {
    fprintf(stderr, "can't chown %s\n", path);
    return;
  }
  lchown(path, uid, gid);

  dir = opendir(path);
  if (! dir)
    return;

  while ((dirent = readdir(dir))) {
    if (dirent->d_name[0] == '.')
      continue;

    sprintf(buf, "%s/%s", path, dirent->d_name);
    chown_rec(buf, uid, gid);
  }

  closedir(dir);
}

int
main(int argc, char **argv)
{
  struct passwd *user;
  char *root;

  if (argc > 2)
    root = argv[2];
  else
    root = "dist-debian";

  if (argv[1] && ! strcmp(argv[1], "-r")) {
    struct stat my_stat;

    stat("build.xml", &my_stat);
      
    chown_rec(root, my_stat.st_uid, my_stat.st_gid);
    return 0;
  }

  user = getpwnam("resin");

  if (! user) {
    fprintf(stderr, "can't find resin");
  }
  
  chown_rec("dist-debian", 0, 0);
  chown_rec("dist-debian/var/www/resin", user->pw_uid, user->pw_gid);
  chown_rec("dist-debian/var/log/resin", user->pw_uid, user->pw_gid);
  chown_rec("dist-debian/usr/local/share/resin", user->pw_uid, user->pw_gid);

  return 0;
}
