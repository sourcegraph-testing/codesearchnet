# coding: utf-8
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
"""Utility class for UserData scripts."""
import re
import os
import logging

from laniakea.core.common import String

logger = logging.getLogger("laniakea")


class UserDataException(Exception):
    """Exception class for Packet Manager."""

    def __init(self, message):
        super().__init__(message)


class UserData:
    """Utility functions for dealing with UserData scripts.
    """
    @staticmethod
    def convert_pair_to_dict(arg):
        """Utility function which transform k=v strings from the command-line into a dict.
        """
        return dict(kv.split('=', 1) for kv in arg)

    @staticmethod
    def parse_only_criterias(conditions):
        result = {}
        for kv in conditions:  # pylint: disable=invalid-name
            k, v = kv.split('=', 1)  # pylint: disable=invalid-name
            if "," in v:
                result[k] = v.split(',', 1)
            else:
                result[k] = [v]
        return result

    @staticmethod
    def convert_str_to_int(arg):
        """
        """
        for k, v in list(arg.items()):  # pylint: disable=invalid-name
            try:
                arg[String(k)] = int(v)
            except ValueError:
                pass
        return arg

    @staticmethod
    def list_tags(userdata):
        """List all used macros within a UserData script.

        :param userdata: The UserData script.
        :type userdata: str
        """
        macros = re.findall('@(.*?)@', userdata)
        logging.info('List of available macros:')
        for macro in macros:
            logging.info('\t%r', macro)

    @staticmethod
    def handle_tags(userdata, macros):
        """Insert macro values or auto export variables in UserData scripts.

        :param userdata: The UserData script.
        :type userdata: str
        :param macros: UserData macros as key value pair.
        :type macros: dict
        :return: UserData script with the macros replaced with their values.
        :rtype: str
        """
        macro_vars = re.findall('@(.*?)@', userdata)
        for macro_var in macro_vars:
            if macro_var == '!all_macros_export':
                macro_var_export_list = []
                for defined_macro in macros:
                    macro_var_export_list.append('export %s="%s"' % (defined_macro, macros[defined_macro]))
                macro_var_exports = "\n".join(macro_var_export_list)

                userdata = userdata.replace('@%s@' % macro_var, macro_var_exports)
            elif macro_var == "!all_macros_docker":
                macro_var_export_list = []
                for defined_macro in macros:
                    macro_var_export_list.append("-e '%s=%s'" % (defined_macro, macros[defined_macro]))
                macro_var_exports = " ".join(macro_var_export_list)

                userdata = userdata.replace('@%s@' % macro_var, macro_var_exports)
            else:
                if "|" in macro_var:
                    macro_var, default_value = macro_var.split('|')
                    if macro_var not in macros:
                        logging.warning('Using default variable value %s for @%s@ ', default_value, macro_var)
                        value = default_value
                    else:
                        value = macros[macro_var]

                    userdata = userdata.replace('@%s|%s@' % (macro_var, default_value), value)
                else:
                    if macro_var not in macros:
                        logging.error('Undefined variable @%s@ in UserData script', macro_var)
                        return None

                    userdata = userdata.replace('@%s@' % macro_var, macros[macro_var])

        return userdata

    @staticmethod
    def handle_import_tags(userdata, import_root):
        """Handle @import(filepath)@ tags in a UserData script.

        :param import_root: Location for imports.
        :type import_root: str
        :param userdata: UserData script content.
        :type userdata: str
        :return: UserData script with the contents of the imported files.
        :rtype: str
        """
        imports = re.findall('@import\((.*?)\)@', userdata)  # pylint: disable=anomalous-backslash-in-string
        if not imports:
            return userdata

        for filepath in imports:
            logger.info('Processing "import" of %s', filepath)
            import_path = os.path.join(import_root, filepath)
            try:
                with open(import_path) as fo:
                    content = fo.read()
                    userdata = userdata.replace('@import(%s)@' % filepath, content)
            except FileNotFoundError:
                raise UserDataException('Import path {} not found.'.format(import_path))

        return userdata
