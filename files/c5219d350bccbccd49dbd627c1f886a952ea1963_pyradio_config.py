# -*- coding: utf-8 -*-
import csv
import sys
import logging
import glob
import curses
import collections
from os import path, getenv, makedirs, remove
from time import ctime
from datetime import datetime
from shutil import copyfile, move
from .log import Log


logger = logging.getLogger(__name__)


class PyRadioStations(object):
    """ PyRadio stations file management """

    stations_file = ''
    stations_filename_only = ''
    stations_filename_only_no_extension = ''
    foreign_filename_only_no_extension = ''
    previous_stations_file = ''

    """ this is always on users config dir """
    stations_dir = ''

    """ True if playlist not in config dir """
    foreign_file = False

    stations = []
    _reading_stations = []
    playlists = []

    selected_playlist = -1
    number_of_stations = -1

    """ new_format: True:  3 columns (name,URL,encoding)
        new_format: False: 2 columns (name,URL) """
    new_format = False

    dirty_playlist = False

    def __init__(self, stationFile=''):

        if sys.platform.startswith('win'):
            self.stations_dir = path.join(getenv('APPDATA'), 'pyradio')
        else:
            self.stations_dir = path.join(getenv('HOME', '~'), '.config', 'pyradio')
        """ Make sure config dir exists """
        if not path.exists(self.stations_dir):
            try:
                makedirs(self.stations_dir)
            except:
                print('Error: Cannot create config directory: "{}"'.format(self.stations_dir))
                sys.exit(1)
        self.root_path = path.join(path.dirname(__file__), 'stations.csv')

        """ If a station.csv file exitst, which is wrong,
            we rename it to stations.csv """
        if path.exists(path.join(self.stations_dir, 'station.csv')):
                copyfile(path.join(self.stations_dir, 'station.csv'),
                        path.join(self.stations_dir, 'stations.csv'))
                remove(path.join(self.stations_dir, 'station.csv'))

        self._move_old_csv(self.stations_dir)
        self._check_stations_csv(self.stations_dir, self.root_path)

    def _move_old_csv(self, usr):
        """ if a ~/.pyradio files exists, relocate it in user
            config folder and rename it to stations.csv, or if
            that exists, to pyradio.csv """

        src = path.join(getenv('HOME', '~'), '.pyradio')
        dst = path.join(usr, 'pyradio.csv')
        dst1 = path.join(usr, 'stations.csv')
        if path.exists(src) and path.isfile(src):
            if path.exists(dst1):
                copyfile(src, dst)
            else:
                copyfile(src, dst1)
            try:
                remove(src)
            except:
                pass

    def _check_stations_csv(self, usr, root):
        ''' Reclocate a stations.csv copy in user home for easy manage.
            E.g. not need sudo when you add new station, etc '''

        if path.exists(path.join(usr, 'stations.csv')):
            return
        else:
            copyfile(root, path.join(usr, 'stations.csv'))

    def copy_playlist_to_config_dir(self):
        """ Copy a foreign playlist in config dir
            Returns:
                -1: error copying file
                 0: success
                 1: playlist renamed
        """
        ret = 0
        st = path.join(self.stations_dir, self.stations_filename_only)
        if path.exists(st):
            ret = 1
            st = datetime.now().strftime("%Y-%m-%d_%H-%M-%S_")
            st = path.join(self.stations_dir, st + self.stations_filename_only)
            try:
                copyfile(self.stations_file, st)
            except:
                if logger.isEnabledFor(logging.ERROR):
                    logger.error('Cannot copy playlist: "{}"'.format(self.stations_file))
                ret = -1
                return
            self._get_playlist_elements(st)
            self.foreign_file = False
        if logger.isEnabledFor(logging.DEBUG):
            logger.debug('Playlist copied to: "{}"'.format(self.stations_filename_only_no_extension))
        return ret

    def is_same_playlist(self, a_playlist):
        """ Checks if a playlist is already loaded """
        if a_playlist == self.stations_file:
            return True
        else:
            return False

    def is_playlist_reloaded(self):
        return self.is_same_playlist(self.previous_stations_file)

    def _is_playlist_in_config_dir(self):
        """ Check if a csv file is in the config dir """
        if path.dirname(self.stations_file) == self.stations_dir:
            self.foreign_file = False
            self.foreign_filename_only_no_extension = ''
        else:
            self.foreign_file = True
            self.foreign_filename_only_no_extension = self.stations_filename_only_no_extension
        self.foreign_copy_asked = False

    def _get_playlist_abspath_from_data(self, stationFile=''):
        """ Get playlist absolute path
            Returns: playlist path, result
              Result is:
                0  -  playlist found
               -2  -  playlist not found
               -3  -  negative number specified
               -4  -  number not found
               """
        ret = -1
        orig_input = stationFile
        if stationFile:
            if stationFile.endswith('.csv'):
                """ relative or absolute path """
                stationFile = path.abspath(stationFile)
            else:
                """ try to find it in config dir """
                stationFile += '.csv'
                stationFile = path.join(self.stations_dir, stationFile)
            if path.exists(stationFile) and path.isfile(stationFile):
                return stationFile, 0
        else:
            for p in [path.join(self.stations_dir, 'pyradio.csv'),
                      path.join(self.stations_dir, 'stations.csv'),
                      self.root_path]:
                if path.exists(p) and path.isfile(p):
                    return p, 0

        if ret == -1:
            """ Check if playlist number was specified """
            if orig_input.replace('-', '').isdigit():
                sel = int(orig_input) - 1
                if sel == -1:
                    stationFile = path.join(self.stations_dir, 'stations.csv')
                    return stationFile, 0
                elif sel < 0:
                    """ negative playlist number """
                    return '', -3
                else:
                    n, f = self.read_playlists()
                    if sel <= n-1:
                        stationFile=self.playlists[sel][-1]
                        return stationFile, 0
                    else:
                        """ playlist number sel does not exit """
                        return '', -4
            else:
                return '', -2

    def read_playlist_file(self, stationFile=''):
        """ Read a csv file
            Returns: number
                x  -  number of stations or
               -1  -  playlist is malformed
               -2  -  playlist not found
               """
        prev_file = self.stations_file
        prev_format = self.new_format
        self.new_format = False

        ret = 0
        stationFile, ret = self._get_playlist_abspath_from_data(stationFile)
        if ret < 0:
            return ret

        self._reading_stations = []
        with open(stationFile, 'r') as cfgfile:
            try:
                for row in csv.reader(filter(lambda row: row[0]!='#', cfgfile), skipinitialspace=True):
                    if not row:
                        continue
                    try:
                        name, url = [s.strip() for s in row]
                        self._reading_stations.append((name, url, ''))
                    except:
                        name, url, enc = [s.strip() for s in row]
                        self._reading_stations.append((name, url, enc))
                        self.new_format = True
            except:
                self._reading_stations = []
                self.new_format = prev_format
                return -1

        self.stations = list(self._reading_stations)
        self._reading_stations = []
        self._get_playlist_elements(stationFile)
        self.previous_stations_file = prev_file
        self._is_playlist_in_config_dir()
        self.number_of_stations = len(self.stations)
        self.dirty_playlist = False
        if logger.isEnabledFor(logging.DEBUG):
            if self.new_format:
                logger.debug('Playlist is in new format')
            else:
                logger.debug('Playlist is in old format')
        return self.number_of_stations

    def _playlist_format_changed(self):
        """ Check if we have new or old format
            and report if format has changed

            Format type can change by editing encoding,
            deleting a non-utf-8 station etc.
        """
        new_format = False
        for n in self.stations:
            if n[2] != '':
                new_format = True
                break
        if self.new_format == new_format:
            return False
        else:
            return True

    def save_playlist_file(self, stationFile=''):
        """ Save a playlist
        Create a txt file and write stations in it.
        Then rename it to final target

        return    0: All ok
                 -1: Error writing file
                 -2: Error renaming file
        """
        if self._playlist_format_changed():
            self.dirty_playlist = True
            self.new_format = not self.new_format

        if stationFile:
            st_file = stationFile
        else:
            st_file = self.stations_file

        if not self.dirty_playlist:
            if logger.isEnabledFor(logging.DEBUG):
                logger.debug('Playlist not modified...')
            return 0

        st_new_file = st_file.replace('.csv', '.txt')

        tmp_stations = self.stations[:]
        tmp_stations.reverse()
        if self.new_format:
            tmp_stations.append([ '# Find lots more stations at http://www.iheart.com' , '', '' ])
        else:
            tmp_stations.append([ '# Find lots more stations at http://www.iheart.com' , '' ])
        tmp_stations.reverse()
        try:
            with open(st_new_file, 'w') as cfgfile:
                writter = csv.writer(cfgfile)
                for a_station in tmp_stations:
                    writter.writerow(self._format_playlist_row(a_station))
        except:
            if logger.isEnabledFor(logging.DEBUG):
                logger.debug('Cannot open playlist file for writing,,,')
            return -1
        try:
            move(st_new_file, st_file)
        except:
            if logger.isEnabledFor(logging.DEBUG):
                logger.debug('Cannot rename playlist file...')
            return -2
        self.dirty_playlist = False
        return 0

    def _format_playlist_row(self, a_row):
        """ Return a 2-column if in old format, or
            a 3-column row if in new format """
        if self.new_format:
            return a_row
        else:
            return a_row[:-1]

    def _get_playlist_elements(self, a_playlist):
        self.stations_file = path.abspath(a_playlist)
        self.stations_filename_only = path.basename(self.stations_file)
        self.stations_filename_only_no_extension = ''.join(self.stations_filename_only.split('.')[:-1])

    def _bytes_to_human(self, B):
        ''' Return the given bytes as a human friendly KB, MB, GB, or TB string '''
        KB = float(1024)
        MB = float(KB ** 2) # 1,048,576
        GB = float(KB ** 3) # 1,073,741,824
        TB = float(KB ** 4) # 1,099,511,627,776

        if B < KB:
            return '{0} B'.format(B)
        B = float(B)
        if KB <= B < MB:
            return '{0:.2f} KB'.format(B/KB)
        elif MB <= B < GB:
            return '{0:.2f} MB'.format(B/MB)
        elif GB <= B < TB:
            return '{0:.2f} GB'.format(B/GB)
        elif TB <= B:
            return '{0:.2f} TB'.format(B/TB)

    def append_station(self, params, stationFile=''):
        """ Append a station to csv file

        return    0: All ok
                 -2  -  playlist not found
                 -3  -  negative number specified
                 -4  -  number not found
                 -5: Error writing file
                 -6: Error renaming file
        """
        if self.new_format:
            if stationFile:
                st_file = stationFile
            else:
                st_file = self.stations_file

            st_file, ret = self._get_playlist_abspath_from_data(st_file)
            if ret < -1:
                return ret
            try:
                with open(st_file, 'a') as cfgfile:
                    writter = csv.writer(cfgfile)
                    writter.writerow(params)
                return 0
            except:
                return -5
        else:
            self.stations.append([ params[0], params[1], params[2] ])
            self.dirty_playlist = True
            st_file, ret = self._get_playlist_abspath_from_data(stationFile)
            if ret < -1:
                return ret
            ret = self.save_playlist_file(st_file)
            if ret < 0:
                ret -= 4
            return ret

    def remove_station(self, a_station):
        self.dirty_playlist = True
        ret = self.stations.pop(a_station)
        self.number_of_stations = len(self.stations)
        return ret, self.number_of_stations

    def read_playlists(self):
        self.playlists = []
        self.selected_playlist = -1
        files = glob.glob(path.join(self.stations_dir, '*.csv'))
        if len(files) == 0:
            return 0, -1
        else:
            for a_file in files:
                a_file_name = ''.join(path.basename(a_file).split('.')[:-1])
                a_file_size = self._bytes_to_human(path.getsize(a_file))
                a_file_time = ctime(path.getmtime(a_file))
                self.playlists.append([a_file_name, a_file_time, a_file_size, a_file])
        self.playlists.sort()
        """ get already loaded playlist id """
        for i, a_playlist in enumerate(self.playlists):
            if a_playlist[-1] == self.stations_file:
                self.selected_playlist = i
                break
        return len(self.playlists), self.selected_playlist

    def list_playlists(self):
        print('Playlists found in "{}"'.format(self.stations_dir))
        num_of_playlists, selected_playlist = self.read_playlists()
        pad = len(str(num_of_playlists))
        for i, a_playlist in enumerate(self.playlists):
            print('  {0}. {1}'.format(str(i+1).rjust(pad), a_playlist[0]))

    def current_playlist_index(self):
        if logger.isEnabledFor(logging.ERROR):
            if not self.playlists:
                self.read_playlists()
        for i, a_playlist in enumerate(self.playlists):
            if a_playlist[0] == self.stations_filename_only_no_extension:
                return i

class PyRadioConfig(PyRadioStations):

    opts = collections.OrderedDict()
    opts[ 'general_title' ] = [ 'General Options', '' ]
    opts[ 'player' ] = [ 'Player: ', '' ]
    opts[ 'default_playlist' ] = [ 'Def. playlist: ', 'stations' ]
    opts[ 'default_station' ] = [ 'Def station: ', 'False' ]
    opts[ 'default_encoding' ] = [ 'Def. encoding: ', 'utf-8' ]
    opts[ 'connection_timeout' ] = [ 'Connection timeout: ', '10' ]
    opts[ 'theme_title' ] = [ 'Theme Options', '' ]
    opts[ 'theme' ] = [ 'Theme: ', 'dark' ]
    opts[ 'use_transparency' ] = [ 'Use transparency: ', False ]
    opts[ 'playlist_manngement_title' ] = [ 'Playlist Management Options', '' ]
    opts[ 'confirm_station_deletion' ] = [ 'Confirm station deletion: ', True ]
    opts[ 'confirm_playlist_reload' ] = [ 'Confirm playlist reload: ', True ]
    opts[ 'auto_save_playlist' ] = [ 'Auto save playlist: ', False ]
    opts[ 'requested_player' ] = [ '', '' ]
    opts[ 'dirty_config' ] = [ '', False ]

    def __init__(self):
        self.player = ''
        self.requested_player = ''
        self.confirm_station_deletion = True
        self.confirm_playlist_reload = True
        self.auto_save_playlist = False
        self.default_playlist = 'stations'
        self.default_station = 'False'
        self.default_encoding = 'utf-8'
        self.connection_timeout = '10'
        self.theme = 'dark'
        self.use_transparency = False

        self.dirty_config = False

        PyRadioStations.__init__(self)

        self._check_config_file(self.stations_dir)
        self.config_file = path.join(self.stations_dir, 'config')

    @property
    def requested_player(self):
        return self.opts['requested_player'][1]

    @requested_player.setter
    def requested_player(self, val):
        self.opts['requested_player'][1] = val.replace(' ', '')
        if self.opts['player'][1] != self.opts['requested_player'][1]:
            self.opts['player'][1] = self.requested_player
            self.opts['dirty_config'][1] = True

    @property
    def player(self):
        return self.opts['player'][1]

    @player.setter
    def player(self, val):
        self.opts['player'][1] = val
        self.opts['dirty_config'][1] = True

    @property
    def use_transparency(self):
        return self.opts['use_transparency'][1]

    @use_transparency.setter
    def use_transparency(self, val):
        self.opts['use_transparency'][1] = val
        self.opts['dirty_config'][1] = True

    @property
    def default_encoding(self):
        return self.opts['default_encoding'][1]

    @default_encoding.setter
    def default_encoding(self, val):
        self.opts['default_encoding'][1] = val
        self.opts['dirty_config'][1] = True

    @property
    def default_playlist(self):
        return self.opts['default_playlist'][1]

    @default_playlist.setter
    def default_playlist(self, val):
        self.opts['default_playlist'][1] = val
        self.opts['dirty_config'][1] = True

    @property
    def default_station(self):
        return self.opts['default_station'][1]

    @default_station.setter
    def default_station(self, val):
        self.opts['default_station'][1] = val
        self.opts['dirty_config'][1] = True

    @property
    def confirm_station_deletion(self):
        return self.opts['confirm_station_deletion'][1]

    @confirm_station_deletion.setter
    def confirm_station_deletion(self, val):
        self.opts['confirm_station_deletion'][1] = val
        self.opts['dirty_config'][1] = True

    @property
    def confirm_playlist_reload(self):
        return self.opts['confirm_playlist_reload'][1]

    @confirm_playlist_reload.setter
    def confirm_playlist_reload(self, val):
        self.opts['confirm_playlist_reload'][1] = val
        self.opts['dirty_config'][1] = True

    @property
    def auto_save_playlist(self):
        return self.opts['auto_save_playlist'][1]

    @auto_save_playlist.setter
    def auto_save_playlist(self, val):
        self.opts['auto_save_playlist'][1] = val
        self.opts['dirty_config'][1] = True

    @property
    def connection_timeout(self):
        return self.opts['connection_timeout'][1]

    @connection_timeout.setter
    def connection_timeout(self, val):
        self.opts['connection_timeout'][1] = val
        self.opts['dirty_config'][1] = True

    @property
    def theme(self):
        return self.opts['theme'][1]

    @theme.setter
    def theme(self, val):
        self.opts['theme'][1] = val
        self.opts['dirty_config'][1] = True

    @property
    def dirty_config(self):
        return self.opts['dirty_config'][1]

    @dirty_config.setter
    def dirty_config(self, val):
        self.opts['dirty_config'][1] = val

    def _check_config_file(self, usr):
        ''' Make sure a config file exists in the config dir '''
        package_config_file = path.join(path.dirname(__file__), 'config')
        user_config_file = path.join(usr, 'config')

        ''' restore config from bck file '''
        if path.exists(user_config_file + '.restore'):
            try:
                copyfile(user_config_file + '.restore', user_config_file)
                remove(self.user_config_file + '.restore')
            except:
                pass

        ''' Copy package config into user dir '''
        if not path.exists(user_config_file):
            copyfile(package_config_file, user_config_file)

    def read_config(self):
        lines = []
        try:
            with open(self.config_file, 'r') as cfgfile:
                lines = [line.strip() for line in cfgfile if line.strip() and not line.startswith('#') ]

        except:
            self.__dirty_config = False
            return -1
        for line in lines:
            sp = line.replace(' ', '').split('=')
            if len(sp) != 2:
                return -2
            if sp[1] == '':
                return -2
            if sp[0] == 'player':
                self.opts['player'][1] = sp[1].lower().strip()
            elif sp[0] == 'connection_timeout':
                self.opts['connection_timeout'][1] = sp[1].strip()
            elif sp[0] == 'default_encoding':
                self.opts['default_encoding'][1] = sp[1].strip()
            elif sp[0] == 'theme':
                self.opts['theme'][1] = sp[1].strip()
            elif sp[0] == 'default_playlist':
                self.opts['default_playlist'][1] = sp[1].strip()
            elif sp[0] == 'default_station':
                st = sp[1].strip()
                if st == '-1' or st.lower() == 'false':
                    self.opts['default_station'][1] = 'False'
                elif st == "0" or st == 'random':
                    self.opts['default_station'][1] = None
                else:
                    self.opts['default_station'][1] = st
            elif sp[0] == 'confirm_station_deletion':
                if sp[1].lower() == 'false':
                    self.opts['confirm_station_deletion'][1] = False
                else:
                    self.opts['confirm_station_deletion'][1] = True
            elif sp[0] == 'confirm_playlist_reload':
                if sp[1].lower() == 'false':
                    self.opts['confirm_playlist_reload'][1] = False
                else:
                    self.opts['confirm_playlist_reload'][1] = True
            elif sp[0] == 'auto_save_playlist':
                if sp[1].lower() == 'true':
                    self.opts['auto_save_playlist'][1] = True
                else:
                    self.opts['auto_save_playlist'][1] = False
            elif sp[0] == 'use_transparency':
                if sp[1].lower() == 'true':
                    self.opts['use_transparency'][1] = True
                else:
                    self.opts['use_transparency'][1] = False
        self.opts['dirty_config'][1] = False
        return 0

    def save_config(self):
        """ Save config file

            Creates config.restore (back up file)
            Returns:
                -1: Error saving config
                 0: Config saved successfully
                 1: Config not saved (not modified"""
        if not self.opts['dirty_config'][1]:
            if logger.isEnabledFor(logging.INFO):
                logger.info('Config not saved (not modified)')
            return 1
        txt ='''# PyRadio Configuration File

# Player selection
# This is the equivalent to the -u , --use-player command line parameter
# Specify the player to use with PyRadio, or the player detection order
# Example:
#   player = vlc
# or
#   player = vlc,mpv, mplayer
# Default value: mpv,mplayer,vlc
player = {0}

# Default playlist
# This is the playlist to open if none is specified
# You can scecify full path to CSV file, or if the playlist is in the
# config directory, playlist name (filename without extension) or
# playlist number (as reported by -ls command line option)
# Default value: stations
default_playlist = {1}

# Default station
# This is the equivalent to the -p , --play command line parameter
# The station number within the default playlist to play
# Value is 1..number of stations, "-1" or "False" means no auto play
# "0" or "Random" means play a random station
# Default value: False
default_station = {2}

# Default encoding
# This is the encoding used by default when reading data provided by
# a station (such as song title, etc.) If reading said data ends up
# in an error, 'utf-8' will be used instead.
#
# A valid encoding list can be found at:
#   https://docs.python.org/2.7/library/codecs.html#standard-encodings
# replacing 2.7 with specific version:
#   3.0 up to current python version.
#
# Default value: utf-8
default_encoding = {3}

# Connection timeout
# PyRadio will wait for this number of seconds to get a station/server
# message indicating that playback has actually started.
# If this does not happen (within this number of seconds after the
# connection is initiated), PyRadio will consider the station
# unreachable, and display the "Failed to connect to: [station]"
# message.
#
# Valid values: 5 - 60
# Default value: 10
connection_timeout = {4}

# Default theme
# Hardcooded themes:
#   dark (default) (8 colors)
#   light (8 colors)
#   dark_16_colors (16 colors dark theme alternative)
#   light_16_colors (16 colors light theme alternative)
#   black_on_white (bow) (256 colors)
#   white_on_black (wob) (256 colors)
# Default value = 'dark'
theme = {5}

# Transparency setting
# If False, theme colors will be used.
# If True and a compositor is running, the stations' window
# background will be transparent. If True and a compositor is
# not running, the terminal's background color will be used.
# Valid values: True, true, False, false
# Default value: False
use_transparency = {6}


# Playlist management
#
# Specify whether you will be asked to confirm
# every station deletion action
# Valid values: True, true, False, false
# Default value: True
confirm_station_deletion = {7}

# Specify whether you will be asked to confirm
# playlist reloading, when the playlist has not
# been modified within Pyradio
# Valid values: True, true, False, false
# Default value: True
confirm_playlist_reload = {8}

# Specify whether you will be asked to save a
# modified playlist whenever it needs saving
# Valid values: True, true, False, false
# Default value: False
auto_save_playlist = {9}

'''
        copyfile(self.config_file, self.config_file + '.restore')
        if self.opts['default_station'][1] is None:
            self.opts['default_station'][1] = '-1'
        try:
            with open(self.config_file, 'w') as cfgfile:
                cfgfile.write(txt.format(self.opts['player'][1],
                    self.opts['default_playlist'][1],
                    self.opts['default_station'][1],
                    self.opts['default_encoding'][1],
                    self.opts['connection_timeout'][1],
                    self.opts['theme'][1],
                    self.opts['use_transparency'][1],
                    self.opts['confirm_station_deletion'][1],
                    self.opts['confirm_playlist_reload'][1],
                    self.opts['auto_save_playlist'][1]))
        except:
            if logger.isEnabledFor(logging.ERROR):
                logger.error('Error saving config')
            return -1
        try:
            remove(self.config_file + '.restore')
        except:
            pass
        if logger.isEnabledFor(logging.INFO):
            logger.info('Config saved')
        self.opts['dirty_config'][1] = False
        return 0

    def read_playlist_file(self, stationFile=''):
        if stationFile.strip() == '':
            stationFile = self.default_playlist
        return super(PyRadioConfig, self).read_playlist_file(stationFile)

