# Copyright (c) by it's authors. 
# Some rights reserved. See LICENSE, AUTHORS.

try:
    from collections import OrderedDict
except:
    class OrderedDict(object):
        def __init__(self):
            print "Error: OrderedDict not supported!!"

from operator import itemgetter
from twisted.internet import task, defer
import wallaby.FX as FX
import inspect
from functools import partial
from wallaby.pf.pillow import Pillow
from wallaby.observer import *
import sys, copy, os.path

from wallaby.common.sets import *

class House(object):
    rooms = {}
    _observer = None

    @staticmethod
    def get(room):
        if room in (None, ""): room = "None"

        if not room in House.rooms:
            if FX.appModule:
                app = FX.appModule
                if room in ("__CONFIG__", "__WIDGETQUERY__"): app = "wallaby.apps.inspector"

                try:
                    from twisted.plugin import getCache
                    pkg = __import__(app + '.rooms', globals(), locals(), ["*"], 0)
                    modules = getCache(pkg)

                    if room.lower() in modules:
                        mod = FX.imp(app + '.rooms.' + room.lower())
                        if mod:
                            cls = room.capitalize()

                            if cls in mod.__dict__:
                                ctx = House.rooms[room] = mod.__dict__[cls](room)
                                return ctx
                except:
                    pass
                        
            # No room template found. Create generic room
            House.rooms[room] = Room(room)

        return House.rooms[room]

    @staticmethod
    def destroyRoom(room):
        if room in House.rooms:
            House.rooms[room].destroyPeers()
            del House.rooms[room]

    @staticmethod
    def pillowRooms(room, pillow, feathers):    
        rooms = OrderedSet()
        catchers = House.get(room).catchers(pillow)
        if catchers != None and len(catchers) > 0:
            for c in catchers:
                rooms |= c.rooms(pillow, feathers)

        return rooms

    @staticmethod
    def catch(room, pillow, catcher, passThrower=False, single=False):
        room = House.get(room)
        room.catch(pillow, catcher, passThrower, single)

    @staticmethod
    def uncatch(room, pillow, catcher, passThrower=False):
        room = House.get(room)
        room.uncatch(pillow, catcher, passThrower)

    @staticmethod
    def throw(pillow, feathers="", **ka):
        parts = pillow.split(':')
        House.get(parts[0]).throw(parts[1], feathers, **ka)

    @staticmethod
    def observer(basePath=""):
        if House._observer == None:
            House._observer = Observer()

            app = FX.appModule
            try:
                from twisted.plugin import getCache
                pkg = __import__(app + '.peer', globals(), locals(), ["*"], 0)
                modules = getCache(pkg)

                fqPeers = []

                for m in modules:
                    fqPeers.append(app + '.peer.' + m)

                House._observer.scan(fqPeers)
            except:
                pass

            House._observer.scan()

        return House._observer

    @classmethod
    def initializeAll(cls, onlyNew=False):
        for r in House.rooms.values(): 
            if not r.initialized  or not onlyNew:
                r.initialize()

class Room(object):
    __metaclass__ = Pillow

    NoHandlerFound = Pillow.Out
    Ignore = Pillow.In
    Suggests = Pillow.OutState

    Suggest = Pillow.In
    Initialize = Pillow.InOut

    # registerType = OrderedDict
    # registerType = set
    registerType = list

    def __init__(self, name):
        self.initialized = False

        self._name = name
        self._pillows = {}
        self._singlePillows = {}
        self._pillowGuard = set([self])
        self._inBoundPillows = set()
        self._outBoundPillows = set()
        self._statefullPillows = {}

        self._statefullQueue = {}
        self._normalQueue    = []

        self._dequeueCall    = None

        self._lastConfig = None

        if 'Receiving' in self.__class__.__dict__: 
            self._inBoundPillows |= set(self.__class__.__dict__['Receiving'])

        if 'Sending' in self.__class__.__dict__: 
            self._outBoundPillows |= set(self.__class__.__dict__['Sending'])

        self._allPeers = OrderedSet()
        self._peers = []
        self._loadedPeers = []
        self._dynamicPeers = []

        if 'Peers' in self.__class__.__dict__: 
            self._peers = self.__class__.__dict__['Peers']

        # self._config = {'Peers': []}
        self._unclean = False
        self.catch(Room.In.Ignore, self._ignore)

        self._configViewer = None
        self._configDoc = None

        from wallaby.pf.peer.viewer import Viewer

        # None room is not supported
        if self._name == "None": return

        if self._name != "__CONFIG__":
            House.get("__CONFIG__").catch(Viewer.In.RefreshDone, self.configUpdated)
            House.get("__CONFIG__").catch(Viewer.In.Refresh, self._configChanged)
            House.get("__CONFIG__").catch(Viewer.In.Document, self._configDocChanged)
        else:
            self.catch(Viewer.In.RefreshDone, self.configUpdated)
            self.catch(Viewer.In.Refresh, self._configChanged)
            self.catch(Viewer.In.Document, self._configDocChanged)

        self.catch(Room.In.Suggest, self.throwSuggests)
        self.catch(Room.In.Initialize, self.configUpdated)

        # if self._name not in ("__CONFIG__", "__CREDENTIALS__"):
        #     self._configViewer = Viewer("__CONFIG__", self._configChanged, "rooms." + self._name, raw=True, refreshDoneCallback=self.configUpdated, ignoreCredentials=True)
    def _configDocChanged(self, pillow, doc):
        self._configDoc = doc

        if doc != None and (doc.get("rooms." + self._name) == None or not isinstance(doc.get("rooms." + self._name), dict)):
            doc.set("rooms." + self._name, {"Peers": [{"name":"Debugger", "config": {}}]})
            from wallaby.pf.peer.editor import Editor
            House.get("__CONFIG__").throw(Editor.Out.FieldChanged, "rooms")

        self._configChanged(None, "rooms." + self._name)

    def configUpdated(self, pillow, force=False):
        if self._unclean or force:
            self.initialize()
            self.initializePeers()
            self.throw(Room.Out.Initialize, None)
            self._unclean = False
            self.throwSuggests()

    def throwSuggests(self, pillow=None, feathers=None):
        from wallaby.common.document import Document
        data = {
            "peers": self.suggests(),
            "inBound": sorted(list(self._inBoundPillows)),
            "outBound": sorted(list(self._outBoundPillows)),
            "all": sorted(list(self._inBoundPillows | self._outBoundPillows))
        }

        data['all'].insert(0, "")

        self.throw(Room.Out.Suggests, Document(data=data))

    def _configChanged(self, pillow, path):
        from wallaby.pf.peer.viewer import Viewer
        if not self._configDoc or not Viewer.matchPath("rooms." + self._name, path): return
        config = self._configDoc.get("rooms." + self._name)

        import json
        if config != None and (self._lastConfig == None or json.dumps(config) != json.dumps(self._lastConfig)):
            self._lastConfig = copy.deepcopy(config)
            if 'Peers' in config:
                del self._peers[:] # clear all pre-configured peers
            else: 
                self._peers = []
                config['Peers'] = []

            for mod in config['Peers']:
                if isinstance(mod, dict):
                    self._peers.append(mod)

            self._unclean = True
            self.createPeers()

    def _ignore(self, pillow, feathers):
        pass

    def customPeers(self):
        pass

    def rooms(self, *args):
        rooms = OrderedSet()
        rooms.add(self._name)
        return rooms

    def name(self):
        return self._name

    def initializePeers(self):
        done = set()
        for p in self._dynamicPeers:
            p.initialize()
            done.add(p)

        for p in self._allPeers:
            if p not in done:
                p.initialize()

    def initialize(self):
        self.customPeers()
        self.initialized = True

    def createPeers(self):
        for p in self._dynamicPeers: p.destroy(remove=True)

        self._dynamicPeers = []

        observer = House.observer()

        for dct in self._peers:
            name = dct['name']
            if name is None or name == "None": continue

            args = {}
            if 'config' in dct: args = dct['config']

            if not args:
                try:
                    obj = observer.peer(name, self._name)
                except Exception as e:
                    FX.crit("Exception while creating instance of", name)
                    import traceback 
                    traceback.print_exc(file=sys.stdout)

            elif isinstance(args, dict):
                lst = ()

                obj = observer.peer(name, self._name, *lst, **args)

            if not obj:
                FX.crit("Error creating peer", name, "for room", self._name, "with options", args)
            else:
                FX.info("Successfully created peer", name, "for room", self._name)
                self._dynamicPeers.append(obj)
                
    def uncatch(self, pillow, catcher, passThrower=False):
        if ":" in str(pillow):
            parts = pillow.split(':')
            return House.uncatch(parts[0], parts[1], catcher, passThrower)

        if pillow in self._pillows:
            if (catcher, passThrower) in self._pillows[pillow]:
                if Room.registerType == OrderedDict:
                    del self._pillows[pillow][(catcher, passThrower)]
                elif Room.registerType == list:
                    self._pillows[pillow].remove((catcher, passThrower))
                else:
                    self._pillows[pillow].remove((catcher, passThrower))

        if pillow in self._statefullQueue:
            if (catcher, passThrower) in self._statefullQueue[pillow]:
                self._statefullQueue[pillow].remove( (catcher, passThrower) )

    def catchNow(self, pillow):
        d = defer.Deferred()
        self.catch(pillow, partial(self._catchNow, d), single=True)
        return d

    def _catchNow(self, d, pillow, feathers):
        d.callback(feathers)

    def catch(self, pillow, catcher, passThrower=False, single=False):
        if ":" in str(pillow):
            parts = pillow.split(':')
            if parts[0] != self._name:
                return House.catch(parts[0], parts[1], catcher, passThrower, single)

            pillow = unicode(parts[1])

        pillow = unicode(pillow)

        if not single:
            register = self._pillows
        else:
            register = self._singlePillows 

        if not pillow in register:
            register[pillow] = Room.registerType()

        if Room.registerType == OrderedDict:
            register[pillow][(catcher, passThrower)] = True
        elif Room.registerType == list:
            register[pillow].append((catcher, passThrower))
        else:
            register[pillow].add((catcher, passThrower))

        if pillow in self._statefullPillows:
            self.__enqueueStatefull(catcher, pillow, passThrower)
            self.__dequeueRequest()

    def __dequeueRequest(self):
        if self._dequeueCall == None:
            from twisted.internet import reactor
            self._dequeueCall = reactor.callLater(0, self.__dequeue)

    def __enqueueStatefull(self, catcher, pillow, passThrower):
        if pillow not in self._statefullQueue: self._statefullQueue[pillow] = []
        self._statefullQueue[pillow].append( (catcher, passThrower) )

    def __enqueue(self, catcher, pillow, feathers, passThrower=False, **ka):
        if pillow.endswith("!"):
            self.__enqueueStatefull(catcher, pillow, passThrower)
        else:
            self._normalQueue.append( (catcher, (pillow, feathers), ka) ) 

    def __dequeue(self):
        calls = []
        statefullCnt = 0

        for pillow in self._statefullQueue:
            if pillow not in self._statefullPillows: continue

            for ident, (args, ka) in self._statefullPillows[pillow].items():
                statefullCnt += len(self._statefullQueue[pillow])

                for catcher, passThrower in self._statefullQueue[pillow]:
                    if passThrower and 'me' in ka:
                        call = (catcher, args, {"thrower":ka['me']})
                    else:
                        call = (catcher, args, {})

                    calls.append(call)

        self._statefullQueue = {}

        for call in self._normalQueue:
            calls.append(call)

        # print "[", self._name, "] Queue sizes:", statefullCnt, len(self._normalQueue) 

        self._normalQueue = []

        for catcher, args, ka in calls: 
            try:
                catcher(*args, **ka)
            except Exception as e:
                FX.crit("Exception while throwing pillow")
                import traceback 
                traceback.print_exc(file=sys.stdout)

        self._dequeueCall = None
        if len(self._statefullQueue) + len(self._normalQueue) > 0: self.__dequeueRequest()
        
    def enter(self, peer):
        if not peer:
            return

        self._outBoundPillows = self._outBoundPillows | set(peer.dynamicPillows())

        self._allPeers.add(peer)

        peer = peer.__module__ + '.' + peer.__class__.__name__

        if peer in self._loadedPeers:
            return

        self._loadedPeers.append(peer)
        observer = House.observer()

        self._inBoundPillows = self._inBoundPillows | set(observer.inBoundPillows(peer))
        self._outBoundPillows = self._outBoundPillows | set(observer.outBoundPillows(peer))

    def leave(self, peer):
        if not peer:
            return

        if peer in self._allPeers: self._allPeers.remove(peer)

        peer = peer.__module__ + '.' + peer.__class__.__name__

        if peer not in self._loadedPeers:
            return

        observer = House.observer()

        for pillow in observer.outBoundPillows(peer):
            if pillow in self._outBoundPillows:
                self._outBoundPillows.remove(pillow)

        for pillow in observer.inBoundPillows(peer):
            if pillow in self._inBoundPillows:
                self._inBoundPillows.remove(pillow)

        self._loadedPeers.remove(peer)

    def inBoundPillows(self):
        return self._inBoundPillows

    def outBoundPillows(self):
        return self._outBoundPillows

    def allPeers(self):
        return self._allPeers

    def peers(self):
        return self._loadedPeers

    def destroyPeers(self):
        peers = []
        for p in self._allPeers:
            peers.append(p)
        for p in peers:
            p.destroy(remove=True) 

    def throw(self, pillow, *args, **ka): #TODO: refactor to deferredThrow?
        if ":" in str(pillow):
            House.throw(pillow, *args, **ka)
        else:
            self._throw(pillow, *args, **ka)

    def _throw(self, pillow, feathers="", **ka):
        try:
            wcPeers     = None
            normalPeers = None
            singlePeers = None

            if pillow == None: 
                # print "WARNING - skipping none pillow"
                return

            # decode json feathers
            if feathers != None and isinstance(feathers, (str, unicode)) and feathers.startswith('json:'):
                jsonString = '{"object":' + feathers.replace('json:', '', 1) + '}'
                try: 
                    import json
                    feathers = json.loads(jsonString)
                    feathers = feathers["object"]
                except Exception as e:
                    print "Error while parsing json", jsonString, e
                    feathers = None

            # save statefull pillow for resending
            if pillow.endswith("!"):
                identifier = "__any__"
                try: identifier = feathers.identifier()
                except: pass

                if pillow not in self._statefullPillows: self._statefullPillows[pillow] = {}

                self._statefullPillows[pillow][identifier] = ((pillow, feathers), ka)
                self._statefullQueue[pillow] = []

            # create copies of peers to avoid race conditions
            # FIXME: this can result in calls of unallocated QObject methods!!! 
            if "*" in self._pillows:
                wcPeers = copy.copy(self._pillows["*"])

            if pillow in self._pillows:
                normalPeers = copy.copy(self._pillows[pillow])

            if pillow in self._singlePillows:
                singlePeers = copy.copy(self._singlePillows[pillow])
                del self._singlePillows[pillow]

            for peers in (wcPeers, normalPeers, singlePeers):
                if peers != None and pillow != Room.Out.NoHandlerFound:
                    for (catcher, passThrower) in peers:
                        if passThrower and 'me' in ka:
                            self.__enqueue(catcher, pillow, feathers, passThrower=passThrower, thrower=ka['me'])
                        else:
                            self.__enqueue(catcher, pillow, feathers, passThrower=passThrower)

            # Send a "NoHandlerFound" pillow if not already handled
            if normalPeers == None and singlePeers == None and pillow != Room.Out.NoHandlerFound:
                self._throw(Room.Out.NoHandlerFound, pillow, thrower=self)

        except Exception as e:
            FX.crit("Exception while throwing pillow", pillow)
            import traceback 
            traceback.print_exc(file=sys.stdout)

        self.__dequeueRequest()

    def catchers(self, pillow):
        if ":" in str(pillow):
            room,pillow = pillow.split(':')
                
            return House.get(room).catchers(pillow)
        else:
            return self._catchers(pillow)

    def _catchers(self, pillow):
        catchers = []
        if pillow in self._pillows:
            for (catcher, thrower) in self._pillows[pillow]:
                catchers.append(catcher.im_self)

        return catchers 

    def suggests(self):
        observer = House.observer()
        suggests = {}

        for fqa in self._inBoundPillows:
            if fqa not in self._outBoundPillows:
                peers = observer.outBoundPeers(fqa, True)

                # print "WARNING - pillow not thrown:", fqa

                runtimeObject = False
                
                for h in peers:
                    if observer.objectType(h) == Pillow.Runtime:
                        runtimeObject = True

                if runtimeObject:
                    continue

                try:
                    lst = observer.outBoundPeers(fqa, fq=True)
                    for peer in lst:
                        if peer not in suggests:
                            suggests[peer] = 1
                        else:
                            suggests[peer] += 1
                except: 
                    pass

        for peer in self._allPeers:
            try:
                for s in peer.Suggests:
                    if s not in suggests:
                        suggests[s] = 1
                    else:
                        suggests[s] += 1
            except:
                pass

        for peer in self._allPeers:
            if peer.__class__.__name__ in suggests:
                del suggests[peer.__class__.__name__]

        # for fqa in self._outBoundPillows:
        #     if fqa not in self._inBoundPillows:
        #         peers = observer.inBoundPeers(fqa, True)

        #         runtimeObject = False
        #         
        #         for h in peers:
        #             if observer.objectType(h) == Pillow.Runtime:
        #                 runtimeObject = True

        #         if runtimeObject:
        #             continue

        #         try:
        #             lst = observer.intBoundPeers(fqa, fq=True)
        #             suggests = suggests | set(lst)
        #         except: 
        #             pass

        ret = []
        suggests = sorted(suggests.items(), key=itemgetter(1), reverse=True)

        for peer, value in suggests:
            try:
                lst = peer.split('.')
                peer = lst[-1]
                cls = observer.peerClass(peer)

                description = ""
                config = ""

                try:
                    description = cls.Description
                    if isinstance(description, (list, tuple)):
                        description, config = description
                except: pass

                ret.append({"peer":peer, "description": description, "config": config})
            except:
                pass

        return ret

    def check(self, suggest=False):
        print "Checking room", self._name
        observer = House.observer()

        print self.suggests()

        for fqa in self._inBoundPillows:
            if fqa not in self._outBoundPillows:
                peers = observer.outBoundPeers(fqa, True)

                runtimeObject = False
                
                for h in peers:
                    if observer.objectType(h) == Pillow.Runtime:
                        runtimeObject = True

                if runtimeObject:
                    continue

                print "WARNING - pillow not thrown:", fqa
                if suggest:
                    try:
                        pillow = fqa.split('.')[2]
                        lst = observer.outBoundPeers(fqa, fq=True)
                        if lst: print "\tPossible thowers:", lst
                    except:
                        print "ERROR - split:", pillow

        for fqa in self._outBoundPillows:
            if fqa not in self._inBoundPillows:
                peers = observer.inBoundPeers(fqa, True)

                runtimeObject = False
                
                for h in peers:
                    if observer.objectType(h) == Pillow.Runtime:
                        runtimeObject = True

                if runtimeObject:
                    continue

                print "WARNING - pillow not catched:", fqa
                if suggest:
                    try:
                        pillow = fqa.split('.')[2]
                        lst = observer.inBoundPeers(fqa, fq=True)
                        if lst: print "\tPossible catchers:", lst
                    except:
                        print "ERROR - split:", pillow

        for fqa in self._inBoundPillows:
            if fqa not in self._pillows and fqa + "!" not in self._pillows:
                print "WARNING - pillow not registered:", fqa

    @FX.Property
    def registeredPillows():
        doc = "The registeredPillows property"
        def fget(self):
            return self._pillows

