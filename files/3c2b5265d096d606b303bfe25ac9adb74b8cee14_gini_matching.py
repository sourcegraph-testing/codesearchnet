from fuzzywuzzy import fuzz
import fnmatch,glob

def matches_from_list(item,options,fuzzy=90,fname_match=True,fuzzy_fragment=None,guess=False):
    '''Returns the members of ``options`` that best matches ``item``. Will prioritize
    exact matches, then filename-style matching, then fuzzy matching. Returns a tuple of item,
    index, match type, and fuzziness (if applicable)
    
         :item:             string to match
         :options:          list of examples to test against
         :fuzzy:            integer (out of 100) describing how close to match string
         :fname_match:      use filename globbing to match files?
         :fuzzy_fragment:   if not ``None``, will accept substring matches of
                            at least ``fuzzy_fragment`` fuzziness
         :guess:            if ``True``, shortcut for setting ``fuzzy`` and ``min_fragment``
                            to very lenient options
    '''
    matches = []
    
    if guess:
        fuzzy = min(fuzzy,80)
        fuzzy_fragment = min(fuzzy_fragment,70)
    
    option_not_in = lambda item,match_list: all([x[0]!=item for x in match_list])
    
    # Exact matches
    if item in options:
        matches += [(options[i],i,'exact',None) for i in xrange(len(options)) if options[i].lower()==item.lower()]
        # If we have exact matches, don't bother with fuzzy matching
        return matches
    
    # Filename-style matches
    if fname_match:
        matches += [(x,options.index(x),'fname',None) for x in fnmatch.filter(options,item) if option_not_in(x,matches)]
    
    # Fuzzy matches
    if fuzzy:
        sub_matches = []
        for i in xrange(len(options)):
            r = fuzz.ratio(item.lower(),options[i].lower())
            if r>=fuzzy and option_not_in(options[i],matches):
                sub_matches.append((r,i))
        matches += [(options[x[1]],x[1],'fuzzy',x[0]) for x in sorted(sub_matches)]
    
    # Fragment matches
    if fuzzy_fragment:
        sub_matches = []
        for i in xrange(len(options)):
            r = fuzz.partial_ratio(item.lower(),options[i].lower())
            if r>=fuzzy_fragment and option_not_in(options[i],matches):
                sub_matches.append((r,i))
        matches += [(options[x[1]],x[1],'fuzzy_fragment',x[0]) for x in sorted(sub_matches)]
    
    return matches

def best_match_from_list(item,options,fuzzy=90,fname_match=True,fuzzy_fragment=None,guess=False):
    '''Returns the best match from :meth:`matches_from_list` or ``None`` if no good matches'''
    matches = matches_from_list(item,options,fuzzy,fname_match,fuzzy_fragment,guess)
    if len(matches)>0:
        return matches[0]
    return None

def best_item_from_list(item,options,fuzzy=90,fname_match=True,fuzzy_fragment=None,guess=False):
    '''Returns just the best item, or ``None``'''
    match = best_match_from_list(item,options,fuzzy,fname_match,fuzzy_fragment,guess)
    if match:
        return match[0]
    return None