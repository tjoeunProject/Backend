import unittest
from unittest.mock import patch
from fastapi.testclient import TestClient
import server

class RequestSafety(unittest.TestCase):
    def setUp(self):
        if hasattr(server,'authorize'): server.app.dependency_overrides[server.authorize]=lambda:1
        self.client=TestClient(server.app)
    def tearDown(self): server.app.dependency_overrides.clear()
    def test_invalid_days_are_rejected_before_generation(self):
        with patch.object(server.generator,'generate_full_course',return_value={'optimized_places':[]}) as provider:
            response=self.client.post('/generate',json={'destination':['Seoul'],'days':0})
        self.assertEqual(response.status_code,422)
        provider.assert_not_called()
    def test_empty_destination_is_rejected(self):
        response=self.client.post('/generate',json={'destination':[],'days':1})
        self.assertEqual(response.status_code,422)
    def test_request_limits(self):
        for body in [{'destination':['Seoul'],'days':15}, {'destination':' ','days':1},
                     {'destination':['Seoul']*6,'days':1}, {'destination':['Seoul'],'days':1,'tags':['x']*21}]:
            with self.subTest(body=body):
                self.assertEqual(self.client.post('/generate',json=body).status_code,422)
    def test_invalid_optimizer_coordinates_are_rejected(self):
        for places in [[],[{'lat':91,'lng':127}],[{'lat':'37','lng':127}]]:
            self.assertEqual(self.client.post('/optimize',json={'places':places,'days':1}).status_code,422)
    def test_nearby_nested_limits_are_checked_before_provider(self):
        with patch.object(server.recommender,'get_dining_recommendations') as provider:
            for places in [[],[[]],[[]]*15,[[{'lat':37,'lng':127}]*201],[[{'lat':91,'lng':127}]]]:
                self.assertEqual(self.client.post('/nearby',json={'places':places}).status_code,422)
        provider.assert_not_called()
    def test_large_body_is_rejected_before_provider(self):
        with patch.object(server.generator,'generate_full_course',return_value={'optimized_places':[]}) as provider:
            response=self.client.post('/generate',json={'destination':['Seoul'],'days':1,'tags':['x'*262144]})
        self.assertEqual(response.status_code,413)
        provider.assert_not_called()
    def test_provider_errors_do_not_escape_to_clients(self):
        with patch.object(server.generator,'generate_full_course',side_effect=RuntimeError('fixture-private-provider-detail')):
            response=self.client.post('/generate',json={'destination':['Seoul'],'days':1})
        self.assertEqual(response.status_code,502)
        self.assertNotIn('fixture-private-provider-detail',response.text)

class GuardApi(unittest.TestCase):
    def test_unauthenticated_generation_never_calls_provider(self):
        with patch.object(server.generator,'generate_full_course',return_value={'optimized_places':[]}) as provider:
            response=TestClient(server.app).post('/generate',json={'destination':['Seoul'],'days':1})
        self.assertEqual(response.status_code,401)
        provider.assert_not_called()

if __name__=='__main__': unittest.main()
