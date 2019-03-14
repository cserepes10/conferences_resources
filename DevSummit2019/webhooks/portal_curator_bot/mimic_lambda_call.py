"""
Copyright 2019 Esri
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
   http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

"""
This is a sample script to mimic a lambda call so you don't have to 
constantly update an item to see your webhook get triggered. Copy one
of the payloads that your webhook sends out, and replace it in the 
below function call, and you should have similar behavior
"""

from main import handler
print(
    handler(
        {'info': {'webhookId': '21afe1d7c5af483897d544634ee6b163', 
                  'webhookName': 'test', 
                  'portalURL': 'https://my-portal/', 
                  'when': 1550621192069
                  }, 
        'events': [{'userId': 'a9764663c8e24a1298c829e905ca669e', 
                    'username': 'portaladmin', 
                    'when': 1550621192060, 
                    'operation': 'add', 
                    'source': 'item', 
                    'id': '300972091a98495095735772e2b249b3', 
                    'properties': {}}]},
    None))

