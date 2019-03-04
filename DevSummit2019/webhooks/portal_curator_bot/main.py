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

from enum import Enum
import json
SECRETS = json.load(open("./secrets.json", "r"))

from arcgis.gis import GIS
from arcgis.mapping import WebMap

from email_util import send_email_smtp

def handler(lambda_event, lambda_context):
    """The top level function called when AWS lambda is evoked"""
    gis = get_gis_from_lambda_event(lambda_event)
    for event_json in lambda_event['events']:
        event_handler = ArcGISEnterpriseWebhookEventHandler(event_json, gis)
        event_handler.handle_event()
    return {"status" : 200 }

def get_gis_from_lambda_event(lambda_event):
    """Connects to the portal from the lambda event with the specified
    portal admin username and the password from the `./secrets.json` file
    """
    portal_url = lambda_event["info"]["portalURL"]
    portal_admin_username = "portaladmin" # Replace with your credentials!
    portal_admin_password = SECRETS["portal_admin_password"]
    gis = GIS(portal_url, portal_admin_username, portal_admin_password,
              verify_cert=False)

    # Print debug info
    print("This is the lambda event:")
    print(lambda_event)
    print("This is our GIS:")
    print(gis)

    return gis

class ArcGISEnterpriseWebhookEvents(Enum):
    UNKNOWN = 0
    ALL_ITEMS = 1 # Webhook event `/items`
    ALL_GROUPS = 2 # Webhook event `/groups`
    ALL_USERS = 3 # Webhook event `/users`
    # Define other Enums here for other webhook events!

class ArcGISEnterpriseWebhookEventHandler:
    """The class to parse through the webhook json
    payload from the webhook, infer the type of Enterprise
    webhook event, and act accordingly
    """
    event = ArcGISEnterpriseWebhookEvents.UNKNOWN
    def __init__(self, event_json, gis):
        """Instantiate with the webhook `event_json` dict, and the
        already instantiated GIS inst of our portal
        """
        self._event_json = event_json
        self._gis = gis
        self._email_msg = ""
        self._infer_event()

    def _infer_event(self):
        # Test if webhook event is `/items`
        if ('operation' in self._event_json) and \
           ('source' in self._event_json) and \
           (self._event_json['source'] == 'item'):
               self.event = ArcGISEnterpriseWebhookEvents.ALL_ITEMS

        # Test if webhook event is `/groups`
        if ('operation' in self._event_json) and \
           ('source' in self._event_json) and \
           (self._event_json['source'] == 'group'):
               self.event = ArcGISEnterpriseWebhookEvents.ALL_GROUPS

        # Test if webhook event is `/users`
        if ('operation' in self._event_json) and \
           ('source' in self._event_json) and \
           (self._event_json['source'] == 'user'):
                self.event = ArcGISEnterpriseWebhookEvents.ALL_USERS

    def handle_event(self):
        """Call this function to actually call the code that you've
        written for the type of webhook event. Add more functions
        and enum entries for different type of webhooks 
        """
        if self.event == ArcGISEnterpriseWebhookEvents.ALL_ITEMS:
            return self._try_event_as_all_item()
        elif self.event == ArcGISEnterpriseWebhookEvents.ALL_GROUPS:
            return self._try_event_as_all_group()
        elif self.event == ArcGISEntepriseWebhookEvents.ALL_USERS:
            return self._try_event_as_all_user()

    def _try_event_as_all_item(self):
        """In this example, we have only implemented item update webhook
        behavior. Furthermore, only items of type `Web Map` will work.

        Will check the item that triggered the webhook's metadata, and will
        attempt to "fill in" the missing metadata with operational layer
        information. Will then send out an email of changes made.
        """
        item = self._gis.content.get(self._event_json['id'])
        item_owner = self._gis.users.get(item.owner)
        if item.type.lower() != "web map":
            self.report_message = "Item type must be a Web Map. Failing."
            return False
        else:
            new_thumbnail_path = []
            new_tags = []
            for layer_item in self._get_webmap_layer_items(item):
                if (not item.thumbnail) and \
                   (layer_item.thumbnail) and \
                   (not new_thumbnail_path):
                    # If no item.thumbnail, download the first layer thumbnail
                    new_thumbnail_path = layer_item.download_thumbnail("/tmp/")

                num_tags = len(item.tags)
                min_num_tags = 5
                if num_tags < min_num_tags and len(new_tags) < min_num_tags:
                    # If not enough tags, fill in missing tags
                    num_new_tags_to_fill = MIN_NUM_TAGS - (num_tags + len(new_tags))
                    potential_new_tags = \
                        [tag for tag in layer_item.tags \
                         if tag not in item.tags]
                    new_tags.append(potential_new_tags[:num_new_tags_to_fill])
            
            # If a new thumbnail or new tags are staged for update, call 
            # `item.update()` and send out our email
            update_kwargs = {}
            update_args = []
            if new_thumbnail_path:
                update_kwargs["thumbnail"] = new_thumbnail_path
            if new_tags:
                update_args.append({"tags": new_tags})
            if update_kwargs or update_args:
                item.update(*update_args, **update_kwargs)
                self._send_email_report(new_thumbnail_path, new_tags, item)

    def _get_webmap_layer_items(self, webmap_item):
        output = []
        for layer in WebMap(webmap_item).layers:
            if('itemId' in layer):
                output.append(self._gis.content.get(layer['itemId']))
        return output

    def _send_email_report(self, new_thumbnail_path, new_tags, item):
        """Sends an email report of item updates and item information"""
        report = f"Hello {item.owner.username},<br>"\
                 f"I have noticed some issues with "\
                 f'<a href="{item.homepage}">your item</a>. '\
                 f"Please see the remainder of this email for more info.<br>"\
                 f"<h1>Item Metadata Report</h1>"
        html_list_parts = ""
        if new_thumbnail_path:
            html_list_parts += \
                f"<li>Thumbnail updated to {new_thumbnail_path}</li>"
        if new_tags:
            html_list_pars += \
                "<li> Tags updated to {}".format(", ".join(new_tags))
        html_list_parts += self._get_metadata_report(item)
        report += f"<ul>{html_list_parts}</ul>"
        response = send_email_smtp([item.owner.email], report)
        print(f"Sending email to {item.owner.email} status: {response}")

    def _get_metadata_report(item):
        """Returns <li> list items of if the snippet/description/title
        is not up to standard"""
        output = ""
        MIN_NUM_CHARS = 50

        if not item.title:
            output += "<li>There is no title for this item.</li>\n"

        if not item.description:
            output += "<li>There is no description for this item.</li>\n"
        elif len(item.description) < MIN_NUM_CHARS:
            output += f"<li>Your description is {len(item.description)} " + \
                      f"characters long. Consider making it longer.</li>\n"

        if not item.snippet:
            output += "<li>There is no snippet for this item.</li>\n"
        elif len(item.snippet) < MIN_NUM_CHARS:
            output += f"<li>Your snippet is {len(item.snippet)} " + \
                      f"characters long. Consider making it longer.</li>\n"

    def _try_event_as_all_group(self):
        raise Exception("All group webhook handler not implemented!")

    def _try_event_as_all_user(self):
        raise Exception("All user webhook handler not implemented!")
