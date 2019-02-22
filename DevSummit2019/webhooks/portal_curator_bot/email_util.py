import smtplib
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
import json
SECRETS = json.load(open("./secrets.json", "r"))

def send_email_smtp(recipients, message,
                    subject="Message from CuratorBot!"):
    """Sends the `message` string to all of the emails in the 
    `recipients` list using the configured SMTP email server. 
    """
    try:
        # Set up server and credential variables
        smtp_server_url = "smtp.gmail.com"
        smtp_server_port = 587
        sender = "arcgispyapibot@gmail.com"
        username = sender
        password = SECRETS["smtp_email_password"]

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