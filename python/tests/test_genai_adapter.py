import unittest
from types import SimpleNamespace
from unittest.mock import patch
from modules.enricher import PlaceProcessor

class GenaiAdapter(unittest.TestCase):
 def test_official_client_has_bounded_transport_and_preserves_analysis(self):
  with patch('google.genai.Client') as factory:
   processor=PlaceProcessor('fixture-not-a-real-key')
   factory.assert_called_once()
   self.assertEqual(factory.call_args.kwargs['http_options'].timeout,5000)
   self.assertEqual(factory.call_args.kwargs['http_options'].retry_options.attempts,1)
   factory.return_value.models.generate_content.return_value=SimpleNamespace(text='{"Park":{"duration":75,"best_time":"Morning"}}')
   result=processor.process([{'id':'a','name':'Park','lat':37,'lng':127}])
   self.assertEqual(result[0]['duration_min'],75)
   self.assertEqual(result[0]['best_time'],'Morning')
   self.assertEqual(factory.return_value.models.generate_content.call_args.kwargs['model'],'gemini-2.5-flash-lite')

if __name__=='__main__':unittest.main()
