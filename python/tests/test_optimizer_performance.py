import copy
import unittest
from unittest.mock import patch
from geopy.distance import geodesic
from modules.optimizer_v2 import RouteOptimizer


class OptimizerContract(unittest.TestCase):
    def test_symmetric_distances_are_computed_once_per_pair(self):
        places=[{'id':str(i),'lat':37+i*.01,'lng':127,'day':1} for i in range(6)]
        with patch('modules.optimizer_v2.geodesic',wraps=geodesic) as distance:
            result=RouteOptimizer().optimize(copy.deepcopy(places))
        self.assertEqual(distance.call_count,15)
        route=result['Day 1']['places']
        self.assertEqual([p['id'] for p in route],['5','4','3','2','1','0'])
        self.assertEqual([p['visit_order'] for p in route],[1,2,3,4,5,6])
        self.assertTrue(all(p['dist_from_prev_km']>=0 for p in route))


if __name__=='__main__': unittest.main()
