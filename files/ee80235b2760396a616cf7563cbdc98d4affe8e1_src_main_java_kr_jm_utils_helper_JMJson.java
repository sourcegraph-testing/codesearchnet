package kr.jm.utils.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import kr.jm.utils.exception.JMExceptionManager;
import org.slf4j.Logger;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * The type Jm json.
 */
public class JMJson {

    private static final Logger log =
            org.slf4j.LoggerFactory.getLogger(JMJson.class);
    /**
     * The constant MAP_TYPE_REFERENCE.
     */
    public static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE =
            getMapOrListTypeReference();
    /**
     * The constant LIST_TYPE_REFERENCE.
     */
    public static final TypeReference<List<Object>> LIST_TYPE_REFERENCE =
            getMapOrListTypeReference();
    /**
     * The constant LIST_MAP_TYPE_REFERENCE.
     */
    public static final TypeReference<List<Map<String, Object>>>
            LIST_MAP_TYPE_REFERENCE = getMapOrListTypeReference();

    /**
     * Gets map or list type reference.
     *
     * @param <T> the type parameter
     * @return the map or list type reference
     */
    public static <T> TypeReference<T> getMapOrListTypeReference() {
        return new TypeReference<>() {};
    }

    private static ObjectMapper jsonMapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);

    /**
     * To json string string.
     *
     * @param <D>        the type parameter
     * @param dataObject the data object
     * @return the string
     */
    public static <D> String toJsonString(D dataObject) {
        try {
            return jsonMapper.writeValueAsString(dataObject);
        } catch (Exception e) {
            return JMExceptionManager.handleExceptionAndReturnNull(log, e,
                    "toJsonString", dataObject);
        }
    }

    /**
     * To json string string.
     *
     * @param jsonFile the json file
     * @return the string
     */
    public static String toJsonString(File jsonFile) {
        return JMFiles.readString(jsonFile);
    }

    /**
     * To json file file.
     *
     * @param jsonString     the json string
     * @param returnJsonFile the return json file
     * @return the file
     */
    public static File toJsonFile(String jsonString, File returnJsonFile) {
        try {
            jsonMapper.writeValue(returnJsonFile, jsonString);
            return returnJsonFile;
        } catch (Exception e) {
            return JMExceptionManager.handleExceptionAndReturnNull(log, e,
                    "toJsonFile", jsonString);
        }
    }

    /**
     * To json file file.
     *
     * @param <D>            the type parameter
     * @param dataObject     the data object
     * @param returnJsonFile the return json file
     * @return the file
     */
    public static <D> File toJsonFile(D dataObject, File returnJsonFile) {
        try {
            jsonMapper.writeValue(returnJsonFile, dataObject);
            return returnJsonFile;
        } catch (Exception e) {
            return JMExceptionManager.handleExceptionAndReturnNull(log, e,
                    "toJsonFile", dataObject);
        }
    }


    /**
     * With bytes t.
     *
     * @param <T>           the type parameter
     * @param bytes         the bytes
     * @param typeReference the type reference
     * @return the t
     */
    public static <T> T withBytes(byte[] bytes,
            TypeReference<T> typeReference) {
        try {
            return jsonMapper.readValue(bytes, typeReference);
        } catch (Exception e) {
            return JMExceptionManager.handleExceptionAndReturnNull(log, e,
                    "withBytes", new String(bytes));
        }
    }

    /**
     * With bytes t.
     *
     * @param <T>   the type parameter
     * @param bytes the bytes
     * @param c     the c
     * @return the t
     */
    public static <T> T withBytes(byte[] bytes, Class<T> c) {
        try {
            return jsonMapper.readValue(bytes, c);
        } catch (Exception e) {
            return JMExceptionManager.handleExceptionAndReturnNull(log, e,
                    "withBytes", new String(bytes));
        }
    }

    /**
     * With json string t.
     *
     * @param <T>           the type parameter
     * @param jsonString    the json string
     * @param typeReference the type reference
     * @return the t
     */
    public static <T> T withJsonString(String jsonString,
            TypeReference<T> typeReference) {
        return withBytes(jsonString.getBytes(), typeReference);
    }

    /**
     * To map map.
     *
     * @param jsonObjectString the json object string
     * @return the map
     */
    public static Map<String, Object> toMap(String jsonObjectString) {
        return withJsonString(jsonObjectString, MAP_TYPE_REFERENCE);
    }

    /**
     * To list list.
     *
     * @param jsonListString the json list string
     * @return the list
     */
    public static List<Object> toList(String jsonListString) {
        return withJsonString(jsonListString, LIST_TYPE_REFERENCE);
    }

    /**
     * To map list list.
     *
     * @param jsonMapListString the json map list string
     * @return the list
     */
    public static List<Map<String, Object>> toMapList(
            String jsonMapListString) {
        return withJsonString(jsonMapListString, LIST_MAP_TYPE_REFERENCE);
    }

    /**
     * With json string t.
     *
     * @param <T>        the type parameter
     * @param jsonString the json string
     * @param c          the c
     * @return the t
     */
    public static <T> T withJsonString(String jsonString, Class<T> c) {
        return withBytes(jsonString.getBytes(), c);
    }

    /**
     * With json file t.
     *
     * @param <T>           the type parameter
     * @param jsonFile      the json file
     * @param typeReference the type reference
     * @return the t
     */
    public static <T> T withJsonFile(File jsonFile,
            TypeReference<T> typeReference) {
        try {
            return jsonMapper.readValue(jsonFile, typeReference);
        } catch (Exception e) {
            return JMExceptionManager.handleExceptionAndReturnNull(log, e,
                    "withJsonFile", jsonFile);
        }
    }

    /**
     * With json file t.
     *
     * @param <T>      the type parameter
     * @param jsonFile the json file
     * @param c        the c
     * @return the t
     */
    public static <T> T withJsonFile(File jsonFile, Class<T> c) {
        try {
            return jsonMapper.readValue(jsonFile, c);
        } catch (Exception e) {
            return JMExceptionManager.handleExceptionAndReturnNull(log, e,
                    "withJsonFile", jsonFile);
        }
    }

    /**
     * With json input stream t.
     *
     * @param <T>           the type parameter
     * @param inputStream   the input stream
     * @param typeReference the type reference
     * @return the t
     */
    public static <T> T withJsonInputStream(InputStream inputStream,
            TypeReference<T> typeReference) {
        try {
            return jsonMapper.readValue(inputStream, typeReference);
        } catch (Exception e) {
            return JMExceptionManager.handleExceptionAndReturnNull(log, e,
                    "withJsonInputStream", inputStream);
        }
    }

    /**
     * With json input stream t.
     *
     * @param <T>         the type parameter
     * @param inputStream the input stream
     * @param c           the c
     * @return the t
     */
    public static <T> T withJsonInputStream(InputStream inputStream,
            Class<T> c) {
        try {
            return jsonMapper.readValue(inputStream, c);
        } catch (Exception e) {
            return JMExceptionManager.handleExceptionAndReturnNull(log, e,
                    "withJsonInputStream", inputStream);
        }
    }

    /**
     * With json resource t.
     *
     * @param <T>               the type parameter
     * @param resourceClasspath the resource classpath
     * @param typeReference     the type reference
     * @return the t
     */
    public static <T> T withJsonResource(String resourceClasspath,
            TypeReference<T> typeReference) {
        return withJsonInputStream(
                JMResources.getResourceInputStream(resourceClasspath),
                typeReference);
    }

    /**
     * With json resource t.
     *
     * @param <T>               the type parameter
     * @param resourceClasspath the resource classpath
     * @param c                 the c
     * @return the t
     */
    public static <T> T withJsonResource(String resourceClasspath,
            Class<T> c) {
        return withJsonInputStream(
                JMResources.getResourceInputStream(resourceClasspath), c);
    }

    /**
     * With rest or classpath or file path t.
     *
     * @param <T>                                  the type parameter
     * @param resourceRestUrlOrClasspathOrFilePath the resource rest url or classpath or file path
     * @param typeReference                        the type reference
     * @return the t
     */
    public static <T> T withRestOrClasspathOrFilePath(
            String resourceRestUrlOrClasspathOrFilePath,
            TypeReference<T> typeReference) {
        return withJsonString(
                JMRestfulResource.getStringWithRestOrClasspathOrFilePath(
                        resourceRestUrlOrClasspathOrFilePath),
                typeReference);
    }

    /**
     * With rest or file path or classpath t.
     *
     * @param <T>                               the type parameter
     * @param resourceRestOrFilePathOrClasspath the resource rest or file path or classpath
     * @param typeReference                     the type reference
     * @return the t
     */
    public static <T> T withRestOrFilePathOrClasspath(
            String resourceRestOrFilePathOrClasspath,
            TypeReference<T> typeReference) {
        return withJsonString(
                JMRestfulResource.getStringWithRestOrFilePathOrClasspath(
                        resourceRestOrFilePathOrClasspath),
                typeReference);
    }

    /**
     * With classpath or file path t.
     *
     * @param <T>                         the type parameter
     * @param resourceClasspathOrFilePath the resource classpath or file path
     * @param typeReference               the type reference
     * @return the t
     */
    public static <T> T withClasspathOrFilePath(
            String resourceClasspathOrFilePath,
            TypeReference<T> typeReference) {
        return withJsonString(JMResources.getStringWithClasspathOrFilePath(
                resourceClasspathOrFilePath), typeReference);
    }

    /**
     * With file path or classpath t.
     *
     * @param <T>                         the type parameter
     * @param resourceFilePathOrClasspath the resource file path or classpath
     * @param typeReference               the type reference
     * @return the t
     */
    public static <T> T withFilePathOrClasspath(
            String resourceFilePathOrClasspath,
            TypeReference<T> typeReference) {
        return withJsonString(JMResources
                        .getStringWithFilePathOrClasspath(resourceFilePathOrClasspath),
                typeReference);
    }

    /**
     * Transform to map map.
     *
     * @param <T>    the type parameter
     * @param object the object
     * @return the map
     */
    public static <T> Map<String, Object> transformToMap(T object) {
        return transform(object, MAP_TYPE_REFERENCE);
    }

    /**
     * Transform t 2.
     *
     * @param <T1>          the type parameter
     * @param <T2>          the type parameter
     * @param object        the object
     * @param typeReference the type reference
     * @return the t 2
     */
    public static <T1, T2> T2 transform(T1 object,
            TypeReference<T2> typeReference) {
        try {
            return jsonMapper.convertValue(object, typeReference);
        } catch (Exception e) {
            return JMExceptionManager.handleExceptionAndReturnNull(log, e,
                    "transform", object);
        }
    }

    /**
     * Transform t 2.
     *
     * @param <T1>      the type parameter
     * @param <T2>      the type parameter
     * @param object    the object
     * @param typeClass the type class
     * @return the t 2
     */
    public static <T1, T2> T2 transform(T1 object, Class<T2> typeClass) {
        try {
            return jsonMapper.convertValue(object, typeClass);
        } catch (Exception e) {
            return JMExceptionManager.handleExceptionAndReturnNull(log, e,
                    "transform", object);
        }
    }

    /**
     * To pretty string string.
     *
     * @param jsonString the json string
     * @return the string
     */
    public static String toPrettyString(String jsonString) {
        return toPrettyJsonString(
                withJsonString(jsonString, Object.class));
    }

    /**
     * To pretty json string string.
     *
     * @param object the object
     * @return the string
     */
    public static String toPrettyJsonString(Object object) {
        try {
            return jsonMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return JMExceptionManager.handleExceptionAndReturnNull(log, e,
                    "toPrettyJsonString", object);
        }
    }


}
