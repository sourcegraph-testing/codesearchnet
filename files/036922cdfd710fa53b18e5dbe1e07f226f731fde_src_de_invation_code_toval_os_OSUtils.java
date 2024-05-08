/*
 * Copyright (c) 2015, Thomas Stocker
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted (subject to the limitations in the disclaimer
 * below) provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * Neither the name of IIG Telematics, Uni Freiburg nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY
 * THIS LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BELIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.invation.code.toval.os;

import de.invation.code.toval.validate.Validate;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

/**
 * Utils class regarding operating system functionalities and properties.
 *
 * @version 1.0
 * @author Adrian Lange <lange@iig.uni-freiburg.de>
 * @param <O> Return value type of associated applications for a file extension.
 */
public abstract class OSUtils<O extends Object> {

    /**
     * Returns the current operating system
     *
     * @return Operating system of type {@link OSType}
     */
    public static OSType getCurrentOS() {
        String osName = System.getProperty("os.name");
        return OSType.getOSTypeByName(osName);
    }

    /**
     * Returns the execution path to the direcory of the current Java
     * application.
     *
     * @return Execution path as {@link File}.
     * @throws OSException If the execution path can't be determined.
     */
    public static File getExecutionPath() throws OSException {
        try {
            return new File(OSUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
        } catch (URISyntaxException ex) {
            throw new OSException(ex);
        }
    }

    /**
     * Returns the associated application for a given extension.
     *
     * @param fileTypeExtension File extension with leading dot, e.g.
     * <code>.bar</code>.
     * @return {@link String} with path to associated application or
     * <code>null</code> if file extension is not registered or can't be read.
     * @throws OSException
     */
    public abstract O getFileExtension(String fileTypeExtension) throws OSException;

    /**
     * Returns the version of the JVM
     *
     * @return Java specification version
     */
    public static String getJavaVersion() {
        return System.getProperty("java.specification.version");
    }

    /**
     * Returns the current user's home directory.
     *
     * @return The home directory as {@link File}
     */
    public static File getUserHomeDirectory() {
        return new File(System.getProperty("user.home"));
    }

    /**
     * Returns <code>true</code> if this class type fits the operating system,
     * otherwise <code>false</code>.
     *
     * @return Boolean value if class is applicable.
     */
    public abstract boolean isApplicable();

    /**
     * Returns an instance of OSUtils fitting the current operating system or
     * throws an {@link UnsupportedOperationException} if the OSUtils class is
     * not implemented yet for the current operating system.
     *
     * @return Instance of OSUtils
     */
    public static OSUtils getOSUtils() {
        switch (getCurrentOS()) {
            case OS_LINUX:
                return LinuxUtils.instance();
            case OS_MACOSX:
                return MacOSUtils.instance();
            case OS_SOLARIS:
                return SolarisUtils.instance();
            case OS_WINDOWS:
                return WindowsUtils.instance();
            default:
                throw new UnsupportedOperationException("Operating system is not implemented and supported yet.");
        }
    }

    /**
     * Checks if the given extension is already registered.
     *
     * @param fileTypeExtension File extension with leading dot, e.g.
     * <code>.bar</code>.
     * @return <code>true</code> if extension is registered, <code>false</code>
     * otherwise.
     * @throws OSException
     */
    public abstract boolean isFileExtensionRegistered(String fileTypeExtension) throws OSException;

    /**
     * Registers a new file extension in the operating system.
     *
     * @param fileTypeName Name of the file extension. Must be atomic, e.g.
     * <code>foocorp.fooapp.v1</code>.
     * @param fileTypeExtension File extension with leading dot, e.g.
     * <code>.bar</code>.
     * @param application Path to the application, which should open the new
     * file extension.
     * @return <code>true</code> if registration was successful,
     * <code>false</code> otherwise.
     * @throws OSException
     */
    public abstract boolean registerFileExtension(String fileTypeName, String fileTypeExtension, String application) throws OSException;

    /**
     * Runs a command on the operating system.
     *
     * @param command String array of command parts. The parts are concatenated
     * with spaces between the parts. The single command parts should not
     * contain whitespaces.
     * @param inputHandler {@link GenericHandler} to handle the process input
     * stream {@link BufferedReader}.
     * @param errorHandler {@link GenericHandler} to handle the process error
     * output {@link BufferedReader}.
     * @throws OSException
     */
    public void runCommand(String[] command, GenericHandler<BufferedReader> inputHandler, GenericHandler<BufferedReader> errorHandler) throws OSException {
        Validate.notNull(command);

        try {
            Process p = Runtime.getRuntime().exec(command);
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            if (inputHandler != null) {
                inputHandler.handle(in);
            }
            if (errorHandler != null) {
                errorHandler.handle(err);
            }

            in.close();
            p.waitFor();
            p.exitValue();
        } catch (Exception ex) {
            throw new OSException(ex.getMessage(), ex);
        }
    }

    /**
     * Sanitizes file extension such that it can be used in the Windows
     * Registry.
     *
     * @param fileTypeExtension Extension to sanitize
     * @return Sanitized file extension
     * @throws OSException If the file extension is malformed
     */
    protected static String sanitizeFileExtension(String fileTypeExtension) throws OSException {
        // Remove whitespaces
        String newFileTypeExtension = fileTypeExtension.replaceAll("\\s+", "");

        // to lower case
        newFileTypeExtension = newFileTypeExtension.toLowerCase();

        /*
         * Check if name contains multiple dots and if so, just take the last
         * part. Also adds a dot before last part.
         */
        String[] splittedFileExtension = newFileTypeExtension.split("\\.");
        if (newFileTypeExtension.length() == 0 || splittedFileExtension.length == 0) {
            throw new OSException("The given file extension \"" + fileTypeExtension + "\" is too short.");
        }
        newFileTypeExtension = "." + splittedFileExtension[splittedFileExtension.length - 1];

        return newFileTypeExtension;
    }

    /**
     * <p>
     * Generic handler interface. Has a method to handle the given generic type.
     * </p>
     *
     * @param <T> Type of the object to handle.
     *
     * @version 1.0
     * @author Adrian Lange <lange@iig.uni-freiburg.de>
     */
    public interface GenericHandler<T extends Object> {

        /**
         * Method to handle an object of the parameter <code>&#60;T&#62;</code>.
         *
         * @param parameter Object to handle.
         * @throws Exception
         */
        public void handle(T parameter) throws Exception;
    }
}
