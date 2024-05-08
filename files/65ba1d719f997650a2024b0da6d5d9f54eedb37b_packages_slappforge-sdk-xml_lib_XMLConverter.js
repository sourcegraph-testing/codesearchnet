/*
 * Copyright (c) 2017-2018 SLAppForge Lanka Private Ltd. (https://www.slappforge.com).
 *
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

let xmlJS = require('xml-js');

/**
 * @author Udith Gunaratna
 */
module.exports = function () {

    /**
     * Converts the given XML string to the corresponding JSON string
     *
     * @param xmlString             XML string to be converted
     * @param compactOutput         whether to generate compact output (default is true)
     * @param options {XML2JSON}    additional options to be used in conversion
     * @return {string} of JSON
     */
    this.convertToJsonStr = function (xmlString, compactOutput = true, options = {}) {
        options['compact'] = compactOutput;
        return xmlJS.xml2json(xmlString, options);
    };

    /**
     * Converts the given XML string to the corresponding Javascript object
     *
     * @param xmlString         XML string to be converted
     * @param compactOutput     whether to generate compact output (default is true)
     * @param options {XML2JS}  additional options to be used in conversion
     * @return {any} Javascript object
     */
    this.convertToJSObject = function (xmlString, compactOutput = true, options = {}) {
        options['compact'] = compactOutput;
        return xmlJS.xml2js(xmlString, options);
    };
};