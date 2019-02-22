import json
SECRETS = json.load(open("./secrets.json", "r"))

from arcgis.gis import GIS
from arcgis.mapping import WebMap

from email_util import send_email_smtp
#from spell_check_util import spell_check

def handler(lambda_event, lambda_context):
    gis = get_gis_from_lambda_event(lambda_event)
    for event in lambda_event['events']:
        message = try_event_as_item_add(event, gis)
    return {"status" : 200, "message" : message }

def get_gis_from_lambda_event(lambda_event):
    """Connects to the portal from the lambda event with the specified
    portal admin username and the password from the `./secrets.json` file
    """
    print("This is the event:")
    print(lambda_event)
    portal_url = lambda_event["info"]["portalURL"]
    portal_admin_username = "portaladmin"
    portal_admin_password = SECRETS["portal_admin_password"]
    gis = GIS(portal_url, portal_admin_username, portal_admin_password,
              verify_cert=False)
    print("This is our GIS:")
    print(gis)
    return gis

def try_event_as_item_add(event, gis):
    if ('operation' in event) and \
       ('source' in event) and \
       (event['source'] == 'item'):
        item = gis.content.get(event['id'])
        item_owner = gis.users.get(item.owner)
        if item.type.lower() == "web map":
            report_msg = ""
            for layer_item in get_webmap_layer_items(item, gis):
                thumbnail_url = \
                    try_update_empty_thumbnail(item, layer_item)
                if thumbnail_url:
                    report_msg += f'Thumbnail updated to <img src='\
                                  f'"{thumbnail_url}"><hr>'
                new_tags = \
                    try_update_lacking_tags(item, layer_item)
                if new_tags:
                    report_msg += 'Tags updated to '\
                                  '{}.<hr>'.format(', '.join(new_tags))
                report_msg += get_metadata_report(item)
            if report_msg:
                report_msg = f"<h3>Hello {item_owner.username},</h3>" + \
                             f"<br><br>" + \
                             f"I have noticed some issues with your item " + \
                             f"at {item.homepage}. Please see the rest " + \
                             f"of this email for information.<br><hr><br>" + \
                             f"{report_msg}"
                recepients = [item_owner.email,]
                send_email_smtp(recepients, report_msg)
    return "Success!"

def try_update_empty_thumbnail(item_to_update, item_to_take_thumbnail_from):
    """check `item_to_update` for a thumbnail. If there is none, take from
    the 2nd pos argument (`item_to_take_thumbnail_from`)
    """
    if (not item_to_update.thumbnail) and \
       (item_to_take_thumbnail_from.thumbnail):
        path = item_to_take_thumbnail_from.download_thumbnail("/tmp/")
        item_to_update.update(thumbnail=path)
        return item_to_take_thumbnail_from.get_thumbnail_link()
    return False

def try_update_lacking_tags(item_to_update, item_to_take_tags_from):
    """check `item_to_update` for tags. If there are <5 tags, supplement that
    item with tags from `item_to_take_tags_from`
    """
    MIN_NUM_TAGS = 5
    num_tags = len(item_to_update.tags)
    if(num_tags < MIN_NUM_TAGS):
        num_new_tags_to_fill = MIN_NUM_TAGS - num_tags
        potential_new_tags = [tag for tag in item_to_take_tags_from.tags \
                              if tag not in item_to_update.tags]
        new_tags = potential_new_tags[:num_new_tags_to_fill]
        all_tags_to_update_with = item_to_update.tags + new_tags
        item_to_update.update({'tags' : all_tags_to_update_with})
        return all_tags_to_update_with
    return False

def get_webmap_layer_items(webmap_item, gis):
    output = []
    for layer in WebMap(webmap_item).layers:
        if('itemId' in layer):
            output.append(gis.content.get(layer['itemId']))
    return output

def get_metadata_report(item):
    output = ""
    MIN_NUM_CHARS = 50

    if not item.title:
        output += "There is no title for this item.<hr>"

    if not item.description:
        output += "There is no description for this item.<hr>"
    elif len(item.description) < MIN_NUM_CHARS:
        output += f"Your description is {len(item.description)} " + \
                  f"characters long. Consider making it longer.<hr>"

    if not item.snippet:
        output += "There is no snippet for this item.<hr>"
    elif len(item.snippet) < MIN_NUM_CHARS:
        output += f"Your snippet is {len(item.snippet)} " + \
                  f"characters long. Consider making it longer.<hr>"
 

    return output
