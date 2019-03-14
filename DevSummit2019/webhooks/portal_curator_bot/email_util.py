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

import smtplib
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText

from config import *

def send_email_smtp(recipients, message,
                    subject="Message from CuratorBot!"):
    """Sends the `message` string to all of the emails in the 
    `recipients` list using the configured SMTP email server. 
    """
    try:
        # Set up server and credential variables
        smtp_server_url = SMTP_SERVER_URL
        smtp_server_port = SMTP_SERVER_PORT
        sender = SMTP_SENDER
        username = SMTP_USERNAME
        password = SMTP_PASSWORD

        # Instantiate our server, configure the necessary security
        server = smtplib.SMTP(smtp_server_url, smtp_server_port)
        server.ehlo()
        server.starttls() # Needed if TLS is required w/ SMTP server
        server.login(username, password)
    except Exception as e:
        print("Error setting up SMTP server, couldn't send " +
                    f"message to {recipients}")
        raise e

    # For each recipient, construct the message and attempt to send
    did_succeed = True
    for recipient in recipients:
        try:
            msg = MIMEMultipart('alternative')
            msg['Subject'] = subject
            msg['From'] = sender
            msg['To'] = recipient
            msg.attach(MIMEText(message, "html"))
            server.sendmail(sender, [recipient], msg.as_string())
            print(f"SMTP server returned success for sending email "\
                  f"to {recipient}")
        except Exception as e:
            print(f"Failed sending message to {recipient}")
            print(e)
            did_succeed = False
    
    # Cleanup and return
    server.quit()
    return did_succeed
