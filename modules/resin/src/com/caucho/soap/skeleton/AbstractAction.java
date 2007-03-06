/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.soap.skeleton;

import com.caucho.jaxb.JAXBContextImpl;
import com.caucho.jaxb.JAXBUtil;
import com.caucho.jaxb.skeleton.Property;

import static com.caucho.soap.wsdl.WSDLConstants.*;
import com.caucho.util.L10N;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import static javax.xml.XMLConstants.*;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPFaultException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Invokes a SOAP request on a Java POJO method
 */
public abstract class AbstractAction {
  private final static Logger log = 
    Logger.getLogger(AbstractAction.class.getName());
  private static final L10N L = new L10N(AbstractAction.class);

  private static final HashMap<Method,String> _methodNames 
    = new HashMap<Method,String>();

  protected static final String XML_SCHEMA_PREFIX = "xsd";
  protected static final String TARGET_NAMESPACE_PREFIX = "m";
  protected static final String SOAP_ENCODING_STYLE 
    = "http://schemas.xmlsoap.org/soap/encoding/";

  protected static XMLOutputFactory _xmlOutputFactory;
  protected static XMLInputFactory _xmlInputFactory 
    = XMLInputFactory.newInstance();

  protected final Method _method;
  protected final int _arity;
  protected boolean _isOneway;

  protected String _responseName;
  protected String _operationName;
  protected QName _requestName;
  protected QName _resultName;

  protected final HashMap<String,ParameterMarshal> _bodyArguments
    = new HashMap<String,ParameterMarshal>();
  protected ParameterMarshal[] _bodyArgs;

  protected final HashMap<String,ParameterMarshal> _headerArguments
    = new HashMap<String,ParameterMarshal>();

  protected ParameterMarshal _returnMarshal;

  protected final HashMap<Class,ParameterMarshal> _faults
    = new HashMap<Class,ParameterMarshal>();

  protected final HashMap<QName,ParameterMarshal> _faultNames
    = new HashMap<QName,ParameterMarshal>();

  protected int _headerInputs;
  protected int _bodyInputs;
  protected int _headerOutputs;
  protected int _bodyOutputs;

  protected final JAXBContextImpl _jaxbContext;
  protected final String _targetNamespace;

  protected static XMLOutputFactory getXMLOutputFactory()
  {
    if (_xmlOutputFactory == null) {
      _xmlOutputFactory = XMLOutputFactory.newInstance();
      _xmlOutputFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES,
                                    Boolean.TRUE);
    }

    return _xmlOutputFactory;
  }

  protected AbstractAction(Method method, Method eiMethod,
                           JAXBContextImpl jaxbContext, 
                           String targetNamespace,
                           Marshaller marshaller,
                           Unmarshaller unmarshaller)
    throws JAXBException, WebServiceException
  {
    _method = method;
    _arity = _method.getParameterTypes().length;
    _jaxbContext = jaxbContext;
    _targetNamespace = targetNamespace;  // XXX introspect this from the method
    _isOneway = (method.getAnnotation(Oneway.class) != null);

    // set the names for the input/output messages, portType/operation, and
    // binding/operation.
    _operationName = getWebMethodName(method, eiMethod);
    _responseName = _operationName + "Response";

    //
    // Arguments
    //

    Class[] params = method.getParameterTypes();
    Type[] genericParams = method.getGenericParameterTypes();
    Annotation[][] paramAnn = method.getParameterAnnotations();

    Annotation[][] eiParamAnn = null;
    
    if (eiMethod != null)
      eiParamAnn = eiMethod.getParameterAnnotations();

    ArrayList<ParameterMarshal> headerList = new ArrayList<ParameterMarshal>();
    ArrayList<ParameterMarshal> bodyList = new ArrayList<ParameterMarshal>();
    
    for (int i = 0; i < params.length; i++) {
      boolean isHeader = false;

      String localName = "arg" + i; // As per JAX-WS spec

      QName name = null;
      WebParam.Mode mode = WebParam.Mode.IN;
      WebParam webParam = null;

      for (Annotation ann : paramAnn[i]) {
        if (ann instanceof WebParam) {
          webParam = (WebParam) ann;
          break;
        }
      }

      if (webParam == null && eiParamAnn != null) {
        for (Annotation ann : eiParamAnn[i]) {
          if (ann instanceof WebParam) {
            webParam = (WebParam) ann;
            break;
          }
        }
      }

      if (webParam != null) {
        if (! "".equals(webParam.name()))
          localName = webParam.name();

        if ("".equals(webParam.targetNamespace()))
          name = new QName(_targetNamespace, localName);
        else 
          name = new QName(webParam.targetNamespace(), localName);

        if (params[i].equals(Holder.class)) {
          mode = webParam.mode();

          if (_isOneway) {
            throw new WebServiceException(L.l("Method {0} annotated with @Oneway, but contains output argument", method.getName()));
          }
        }
      }

      if (name == null) 
        name = new QName(localName);

      Type type = JAXBUtil.getActualParameterType(genericParams[i]);
      Property property = _jaxbContext.createProperty(type);

      ParameterMarshal pMarshal
        = ParameterMarshal.create(i, property, name, mode,
                                  marshaller, unmarshaller);

      if (isHeader) {
        if (pMarshal instanceof InParameterMarshal)
          _headerInputs++;
        else if (pMarshal instanceof OutParameterMarshal)
          _headerOutputs++;
        else {
          _headerInputs++;
          _headerOutputs++;
        }

        headerList.add(pMarshal);
        _headerArguments.put(localName, pMarshal);
      }
      else {
        if (pMarshal instanceof InParameterMarshal)
          _bodyInputs++;
        else if (pMarshal instanceof OutParameterMarshal)
          _bodyOutputs++;
        else {
          _bodyInputs++;
          _bodyOutputs++;
        }

        bodyList.add(pMarshal);
        _bodyArguments.put(localName, pMarshal);
      }
    }

    _bodyArgs = new ParameterMarshal[bodyList.size()];
    bodyList.toArray(_bodyArgs);

    // 
    // Return type
    //

    if (! Void.TYPE.equals(method.getReturnType())) {
      if (_isOneway)
        throw new WebServiceException(L.l("Method {0} annotated with @Oneway, but has non-void return", method.getName()));

      Property property = 
        _jaxbContext.createProperty(method.getGenericReturnType());

      WebResult webResult = method.getAnnotation(WebResult.class);

      if (webResult == null && eiMethod != null)
        webResult = eiMethod.getAnnotation(WebResult.class);

      if (webResult != null) {
        String localName = webResult.name();

        if ("".equals(localName))
          localName = "return";

        if ("".equals(webResult.targetNamespace()))
          _resultName = new QName(_targetNamespace, localName);
        else
          _resultName = new QName(webResult.targetNamespace(), localName);
      }
      else
        _resultName = new QName("return"); // XXX namespace?

      _returnMarshal = ParameterMarshal.create(0, property, _resultName, 
                                               WebParam.Mode.OUT,
                                               marshaller, unmarshaller);

      // XXX header return?
      _bodyOutputs++;
    }

    //
    // Exceptions
    //

    Class[] exceptions = method.getExceptionTypes();

    for (Class exception : exceptions) {
      QName faultName = new QName(targetNamespace, 
                                  JAXBUtil.classBasename(exception),
                                  TARGET_NAMESPACE_PREFIX);
      /* XXX check for generated exception classes versus raw exceptions
       * i.e. things like getFaultInfo()
      Property property = jaxbContext.createProperty(exception);
      ParameterMarshal marshal = ParameterMarshal.create(0, 
                                                         property, 
                                                         faultName,
                                                         WebParam.Mode.OUT,
                                                         marshaller, 
                                                         unmarshaller);
      _faults.put(exception, marshal);
      _faultNames.put(faultName, marshal);*/
    }
  }

  public static AbstractAction createAction(Method method, 
                                            JAXBContextImpl jaxbContext, 
                                            String targetNamespace,
                                            Marshaller marshaller,
                                            Unmarshaller unmarshaller)
    throws JAXBException, WebServiceException
  {
    // There are three valid modes in JAX-WS:
    //
    //  1. Document wrapped -- all the parameters and return values 
    //  are encapsulated in a single encoded object (i.e. the document).  
    //  This is selected by
    //    SOAPBinding.style() == DOCUMENT
    //    SOAPBinding.use() == LITERAL
    //    SOAPBinding.parameterStyle() == WRAPPED
    //
    //  2. Document bare -- the method must have at most one input and
    //  one output parameter.  No wrapper objects are created.
    //  This is selected by
    //    SOAPBinding.style() == DOCUMENT
    //    SOAPBinding.use() == LITERAL
    //    SOAPBinding.parameterStyle() == BARE
    //
    //  3. RPC style -- parameters and return values are mapped to
    //  wsdl:parts.  This is selected by:
    //    SOAPBinding.style() == RPC
    //    SOAPBinding.use() == LITERAL
    //    SOAPBinding.parameterStyle() == WRAPPED
    //
    // It seems that "use" is never ENCODED in JAX-WS and is not allowed
    // by WS-I, so we don't allow it either.
    //

    // Check for the SOAPBinding annotation...
    
    // look at the declaring class and method first
    Class cl = method.getDeclaringClass();
    Method eiMethod = null;
    SOAPBinding soapBinding = (SOAPBinding) cl.getAnnotation(SOAPBinding.class);
    
    if (method.isAnnotationPresent(SOAPBinding.class))
      soapBinding = method.getAnnotation(SOAPBinding.class);

    if (soapBinding == null) {
      // Then look at the endpoint interface, if available
      WebService webService = (WebService) cl.getAnnotation(WebService.class);

      if (webService != null) {
        if (! "".equals(webService.endpointInterface())) {
          try {
            ClassLoader loader = cl.getClassLoader();

            Class endpointInterface = 
              loader.loadClass(webService.endpointInterface());

            soapBinding = 
              (SOAPBinding) endpointInterface.getAnnotation(SOAPBinding.class);

            eiMethod = endpointInterface.getMethod(method.getName(), 
                                                   method.getParameterTypes());

            if (eiMethod.isAnnotationPresent(SOAPBinding.class))
              soapBinding = eiMethod.getAnnotation(SOAPBinding.class);
          }
          catch (ClassNotFoundException e) {
            throw new WebServiceException(L.l("Endpoint interface {0} not found", webService.endpointInterface()), e);
          }
          catch (NoSuchMethodException e) {
            // We don't care if the method isn't defined in the interface
          }
        }
      }
    }

    // Document wrapped is the default for methods w/o a @SOAPBinding
    if (soapBinding == null)
      return new DocumentWrappedAction(method, eiMethod, 
                                       jaxbContext, targetNamespace,
                                       marshaller, unmarshaller);

    if (soapBinding.use() == SOAPBinding.Use.ENCODED)
      throw new UnsupportedOperationException(L.l("SOAP encoded style is not supported by JAX-WS"));

    if (soapBinding.style() == SOAPBinding.Style.DOCUMENT) {
      if (soapBinding.parameterStyle() == SOAPBinding.ParameterStyle.WRAPPED)
        return new DocumentWrappedAction(method, eiMethod, 
                                         jaxbContext, targetNamespace,
                                         marshaller, unmarshaller);
      else {
        return new DocumentBareAction(method, eiMethod, 
                                      jaxbContext, targetNamespace,
                                      marshaller, unmarshaller);
      }
    }
    else {
      if (soapBinding.parameterStyle() != SOAPBinding.ParameterStyle.WRAPPED)
        throw new UnsupportedOperationException(L.l("SOAP RPC bare style not supported"));

      return new RpcAction(method, eiMethod, jaxbContext, targetNamespace,
                           marshaller, unmarshaller);
    }
  }

  /**
   * Client-side invocation.
   */
  public Object invoke(String url, Object[] args)
    throws IOException, XMLStreamException, MalformedURLException, 
           JAXBException, Throwable
  {
    URL urlObject = new URL(url);
    URLConnection connection = urlObject.openConnection();

    // XXX HTTPS
    if (! (connection instanceof HttpURLConnection))
      return null;

    HttpURLConnection httpConnection = (HttpURLConnection) connection;

    try {
      //
      // Send the request
      //

      httpConnection.setRequestMethod("POST");
      httpConnection.setDoInput(true);
      httpConnection.setDoOutput(true);

      OutputStream httpOut = httpConnection.getOutputStream();
      XMLStreamWriter out 
        = getXMLOutputFactory().createXMLStreamWriter(httpOut);

      writeRequest(out, args);
      out.flush();

      //
      // Parse the response
      // 

      if (httpConnection.getResponseCode() != 200)
        return null; // XXX more meaningful error

      if (_isOneway)
        return null;

      InputStream httpIn = httpConnection.getInputStream();
      XMLStreamReader in = _xmlInputFactory.createXMLStreamReader(httpIn);

      Object ret = readResponse(in, args);

      return ret;
    } 
    finally {
      if (httpConnection != null)
        httpConnection.disconnect();
    }
  }

  protected void writeRequest(XMLStreamWriter out, Object []args)
    throws IOException, XMLStreamException, JAXBException
  {
    out.writeStartDocument();
    out.writeStartElement(Skeleton.SOAP_ENVELOPE_PREFIX, 
                          "Envelope", 
                          Skeleton.SOAP_ENVELOPE);
    out.writeNamespace(Skeleton.SOAP_ENVELOPE_PREFIX, Skeleton.SOAP_ENVELOPE);

    out.writeStartElement(Skeleton.SOAP_ENVELOPE_PREFIX, 
                          "Header", 
                          Skeleton.SOAP_ENVELOPE);

    for (ParameterMarshal marshal : _headerArguments.values())
      marshal.serializeCall(out, args);

    out.writeEndElement(); // Header

    out.writeStartElement(Skeleton.SOAP_ENVELOPE_PREFIX, 
                          "Body", 
                          Skeleton.SOAP_ENVELOPE);

    writeMethodInvocation(out, args);

    out.writeEndElement(); // Body
    out.writeEndElement(); // Envelope
  }

  abstract protected void writeMethodInvocation(XMLStreamWriter out, 
                                                Object []args)
    throws IOException, XMLStreamException, JAXBException;

  abstract protected Object readResponse(XMLStreamReader in, Object []args)
    throws IOException, XMLStreamException, JAXBException, Throwable;

  /**
   * Invokes the request for a call.
   */
  public void invoke(Object service, XMLStreamReader in, XMLStreamWriter out)
    throws IOException, XMLStreamException, Throwable
  {
    // We're starting out at the point in the input stream where the 
    // method name is listed (with the arguments as children) and the 
    // point in the output stream where the results are to be written.
    
    Object[] args = readMethodInvocation(in);

    Object value = null;

    try {
      value = _method.invoke(service, args);
    } 
    catch (IllegalAccessException e) {
      throw new Throwable(e);
    } 
    catch (IllegalArgumentException e) {
      throw new Throwable(e);
    }
    catch (InvocationTargetException e) {
      writeFault(out, e.getCause());
      return;
    }

    if (! _isOneway)
      writeResponse(out, value, args);
  }

  // reads the method invocation and returns the arguments
  abstract protected Object[] readMethodInvocation(XMLStreamReader in)
    throws IOException, XMLStreamException, JAXBException;

  abstract protected void writeResponse(XMLStreamWriter out, 
                                        Object value, Object[] args)
    throws IOException, XMLStreamException, JAXBException;

  protected void writeFault(XMLStreamWriter out, Throwable fault)
    throws IOException, XMLStreamException, JAXBException
  {
    out.writeStartElement(Skeleton.SOAP_ENVELOPE_PREFIX, 
                          "Fault", 
                          Skeleton.SOAP_ENVELOPE);

    out.writeStartElement("faultcode");
    out.writeCharacters(Skeleton.SOAP_ENVELOPE_PREFIX + ":Server");
    out.writeEndElement(); // faultcode

    //
    // Marshal this exception as a fault.
    // 
    // faults must have exactly the same class as declared on the method,
    // otherwise we emit an internal server error.
    // XXX This may not be behavior required by the standard and we may 
    // be able to improve here by casting as a superclass.
    ParameterMarshal faultMarshal = _faults.get(fault.getClass());

    if (faultMarshal == null) {
      out.writeStartElement("faultstring");
      out.writeCharacters(L.l("Internal server error"));
      out.writeEndElement(); // faultstring
    }
    else {
      out.writeStartElement("faultstring");
      out.writeCharacters(fault.getMessage());
      out.writeEndElement(); // faultstring

      out.writeStartElement("detail");
      faultMarshal.serializeReply(out, fault);
      out.writeEndElement(); // detail 
    }

    out.writeEndElement(); // Fault
  }

  protected Throwable readFault(XMLStreamReader in)
    throws IOException, XMLStreamException, JAXBException
  {
    Throwable fault = null;
    String message = null;

    while (in.nextTag() == XMLStreamReader.START_ELEMENT) {
      if ("faultcode".equals(in.getLocalName())) {
        if (in.next() == XMLStreamReader.CHARACTERS) {
          String code = in.getText();
          int colon = code.indexOf(':');

          if (colon >= 0)
            code = code.substring(colon + 1);

          if ("Server".equalsIgnoreCase(code)) {
            // XXX Do anything with this?
          }
          else if ("Client".equalsIgnoreCase(code)) {
            // XXX Do anything with this?
          }
          else if ("VersionMismatch".equalsIgnoreCase(code)) {
            // XXX Do anything with this?
          }
          else if ("MustUnderstand".equalsIgnoreCase(code)) {
            // XXX Do anything with this?
          }
        }

        while (in.nextTag() != XMLStreamReader.END_ELEMENT) {}
      }
      else if ("faultstring".equals(in.getLocalName())) {
        if (in.next() == XMLStreamReader.CHARACTERS)
          message = in.getText();

        while (in.nextTag() != XMLStreamReader.END_ELEMENT) {}
      }
      else if ("faultactor".equals(in.getLocalName())) {
        // XXX Do anything with this?
        while (in.nextTag() != XMLStreamReader.END_ELEMENT) {}
      }
      else if ("detail".equals(in.getLocalName())) {
        if (in.nextTag() == XMLStreamReader.START_ELEMENT) {
          ParameterMarshal faultMarshal = _faultNames.get(in.getName());

          if (faultMarshal != null)
            fault = (Exception) faultMarshal.deserializeReply(in, fault);
        }

        while (in.nextTag() != XMLStreamReader.END_ELEMENT) {}
      }
    }

    /*
    if (fault == null)
      fault = new SOAPFaultException(soapFault);*/

    return fault;
  }

  public void writeWSDLMessages(XMLStreamWriter out, String soapNamespaceURI)
    throws XMLStreamException
  {
    out.writeStartElement(WSDL_NAMESPACE, "message");
    out.writeAttribute("name", _operationName);

    out.writeEmptyElement(WSDL_NAMESPACE, "part");
    out.writeAttribute("name", "parameters"); // XXX partName?
    out.writeAttribute("element", 
                       TARGET_NAMESPACE_PREFIX + ':' + _operationName);

    out.writeEndElement(); // message

    if (! _isOneway) {
      out.writeStartElement(WSDL_NAMESPACE, "message");
      out.writeAttribute("name", _responseName);

      out.writeEmptyElement(WSDL_NAMESPACE, "part");
      out.writeAttribute("name", "parameters"); // XXX partName?
      out.writeAttribute("element", 
                         TARGET_NAMESPACE_PREFIX + ':' + _responseName);

      out.writeEndElement(); // message
    }
  }
  
  public void writeWSDLOperation(XMLStreamWriter out, String soapNamespaceURI)
    throws XMLStreamException
  {
    out.writeStartElement(WSDL_NAMESPACE, "operation");
    out.writeAttribute("name", _operationName);
    // XXX out.writeAttribute("parameterOrder", "");

    out.writeEmptyElement(WSDL_NAMESPACE, "input");
    out.writeAttribute("message", 
                       TARGET_NAMESPACE_PREFIX + ':' + _operationName);

    if (! _isOneway) {
      out.writeEmptyElement(WSDL_NAMESPACE, "output");
      out.writeAttribute("message", 
                         TARGET_NAMESPACE_PREFIX + ':' + _responseName);
    }

    out.writeEndElement(); // operation
  }

  public void writeWSDLBindingOperation(XMLStreamWriter out, 
                                        String soapNamespaceURI)
    throws XMLStreamException
  {
    out.writeStartElement(WSDL_NAMESPACE, "operation");
    out.writeAttribute("name", _operationName);
    // XXX out.writeAttribute("parameterOrder", "");

    out.writeEmptyElement(soapNamespaceURI, "operation");
    out.writeAttribute("soapAction", "");

    out.writeStartElement(WSDL_NAMESPACE, "input");
    // XXX
    out.writeEmptyElement(soapNamespaceURI, "body");
    out.writeAttribute("use", "literal");

    out.writeEndElement(); // input

    if (! _isOneway) {
      out.writeStartElement(WSDL_NAMESPACE, "output");
      // XXX
      out.writeEmptyElement(soapNamespaceURI, "body");
      out.writeAttribute("use", "literal");

      out.writeEndElement(); // output
    }

    out.writeEndElement(); // operation
  }

  public void writeSchema(XMLStreamWriter out, String namespace)
    throws XMLStreamException
  {
    // XXX header arguments
    
    out.writeEmptyElement(XML_SCHEMA_PREFIX, "element", W3C_XML_SCHEMA_NS_URI);
    out.writeAttribute("name", _operationName);
    out.writeAttribute("type", TARGET_NAMESPACE_PREFIX + ':' + _operationName);

    if (_bodyInputs + _headerInputs == 0) {
      out.writeEmptyElement(XML_SCHEMA_PREFIX, 
                            "complexType", 
                            W3C_XML_SCHEMA_NS_URI);
      out.writeAttribute("name", _operationName);
    }
    else {
      out.writeStartElement(XML_SCHEMA_PREFIX, 
                            "complexType", 
                            W3C_XML_SCHEMA_NS_URI);
      out.writeAttribute("name", _operationName);

      out.writeStartElement(XML_SCHEMA_PREFIX, 
                            "sequence", 
                            W3C_XML_SCHEMA_NS_URI);
      
      for (ParameterMarshal param : _bodyArguments.values()) {
        if (! (param instanceof OutParameterMarshal))
          param.writeElement(out);
      }

      out.writeEndElement(); // sequence

      out.writeEndElement(); // complexType
    }

    out.writeEmptyElement(XML_SCHEMA_PREFIX, "element", W3C_XML_SCHEMA_NS_URI);
    out.writeAttribute("name", _responseName);
    out.writeAttribute("type", TARGET_NAMESPACE_PREFIX + ':' + _operationName);

    if (_bodyOutputs + _headerOutputs == 0) {
      out.writeEmptyElement(XML_SCHEMA_PREFIX, 
                            "complexType", 
                            W3C_XML_SCHEMA_NS_URI);
      out.writeAttribute("name", _responseName);
    }
    else {
      out.writeStartElement(XML_SCHEMA_PREFIX, 
                            "complexType", 
                            W3C_XML_SCHEMA_NS_URI);
      out.writeAttribute("name", _responseName);

      out.writeStartElement(XML_SCHEMA_PREFIX, 
                            "sequence", 
                            W3C_XML_SCHEMA_NS_URI);
      
      if (_returnMarshal != null)
        _returnMarshal.writeElement(out);

      for (ParameterMarshal param : _bodyArguments.values()) {
        if (! (param instanceof InParameterMarshal))
          param.writeElement(out);
      }

      out.writeEndElement(); // sequence

      out.writeEndElement(); // complexType
    }
  }

  public boolean hasHeaderInput()
  {
    return false;
  }

  public int getArity()
  {
    return _arity;
  }

  public String getOperationName()
  {
    return _operationName;
  }

  public static String getWebMethodName(Method method)
  {
    String methodName = _methodNames.get(method);

    if (methodName == null) {
      Method eiMethod = getEIMethod(method);
      methodName = getWebMethodName(method, eiMethod);

      _methodNames.put(method, methodName);
    }

    return methodName;
  }

  public static String getWebMethodName(Method method, Method eiMethod)
  {
    String name = method.getName();

    WebMethod webMethod = method.getAnnotation(WebMethod.class);

    if (webMethod == null && eiMethod != null)
      webMethod = eiMethod.getAnnotation(WebMethod.class);

    if (webMethod != null && ! "".equals(webMethod.operationName()))
      name = webMethod.operationName();
    
    return name;
  }

  public static Method getEIMethod(Method method)
  {
    try {
      Class cl = method.getDeclaringClass();
      WebService webService = (WebService) cl.getAnnotation(WebService.class);

      if (webService != null) {
        if (! "".equals(webService.endpointInterface())) {
          ClassLoader loader = cl.getClassLoader();

          Class endpointInterface = 
            loader.loadClass(webService.endpointInterface());

          return endpointInterface.getMethod(method.getName(), 
                                             method.getParameterTypes());
        }
      }
    }
    catch (ClassNotFoundException e) {
    }
    catch (NoSuchMethodException e) {
    }

    return null;
  }
}
