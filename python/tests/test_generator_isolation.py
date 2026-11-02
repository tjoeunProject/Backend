import copy,unittest
from types import SimpleNamespace
from unittest.mock import patch
from modules.generator import CourseGenerator
from modules.generator_v2 import CourseGeneratorV2
from modules.place_cache import PlaceCache

class GeneratorIsolation(unittest.TestCase):
    def test_cache_ttl_capacity_and_copy_boundaries(self):
        clock=[0]
        cache=PlaceCache(maxsize=2,ttl=10,clock=lambda:clock[0])
        value=[{'name':'original'}]
        cache.put('a',value);value[0]['name']='caller-change'
        self.assertEqual(cache.get('a')[0]['name'],'original')
        returned=cache.get('a');returned[0]['name']='reader-change'
        self.assertEqual(cache.get('a')[0]['name'],'original')
        cache.put('b',[]);cache.put('c',[])
        self.assertIsNone(cache.get('a'))
        clock[0]=10
        self.assertIsNone(cache.get('b'))

    def test_restaurant_photo_does_not_embed_server_key(self):
        generator=CourseGeneratorV2(None,'fixture')
        generator.recommender.maps_key='private-fixture-key'
        pick={'name':'meal','geometry':{'location':{'lat':37,'lng':127}},'photos':[{'photo_reference':'reference'}]}
        result=generator._format_restaurant(pick,{'id':'tour'},'점심')
        self.assertNotIn('private-fixture-key',str(result))
        self.assertEqual(result['photo_reference'],'reference')

    def test_each_region_has_its_own_cached_results(self):
        generator=CourseGenerator(None,'fixture')
        class Search:
            def __init__(self,params,**kwargs): self.region=params['q'].split()[0]
            def get_dict(self):
                return {'local_results':[{'place_id':self.region,'title':self.region,'gps_coordinates':{'latitude':37,'longitude':127}}]}
        with patch('modules.generator.GoogleSearch',Search):
            result=generator._search_places_by_regions(['A','B'],limit=1)
            self.assertEqual([p['region'] for p in result],['A','B'])
            result[0]['name']='modified'
            again=generator._search_places_by_regions(['A'],limit=1)
            self.assertEqual(again[0]['name'],'A')

    def test_dining_uses_numeric_distance_and_request_local_duplicates(self):
        generator=CourseGeneratorV2(None,'fixture')
        restaurant={'place_id':'meal','name':'Fixture restaurant','geometry':{'location':{'lat':37,'lng':127}}}
        class Maps:
            def places(self,**kwargs): return {'results':[copy.deepcopy(restaurant)]}
        generator.recommender.gmaps=Maps()
        place={'id':'tour','name':'Tour','lat':37,'lng':127,'duration_min':180}
        for _ in range(2):
            result=generator._insert_dining_simulation([copy.deepcopy(place)],[],1,set())
            self.assertEqual([p['name'] for p in result],['Tour','Fixture restaurant'])

if __name__=='__main__': unittest.main()
