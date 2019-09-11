# Seafile Zimlet
Here you can find the installer and sources for the Seafile modification of the Zimbra WebDAV Client/Nextcloud Zimlet. For more information refer to the readme here: https://github.com/Zimbra-Community/owncloud-zimlet


### Installing

    wget --no-cache https://raw.githubusercontent.com/Zimbra-Community/seafile/master/webdav-client-installer.sh -O /tmp/webdav-client-installer.sh
    chmod +rx /tmp/webdav-client-installer.sh
    /tmp/webdav-client-installer.sh 

### Known issues

1. UI allows saving to root folders (Libraries) but this does not work in Seafile. No error is displayed in case the user saves attachments to the root, but they won't succeed.
2. Seafile has a minimum length for sharing passwords. But there is no check on this in the Zimlet. (https://manual.seafile.com/config/seahub_settings_py.html; SHARE_LINK_PASSWORD_MIN_LENGTH = 8). If the password is to short there is an error.
You can either leave it as is, and confront your users with `Cannot create share` error or set the minimum length of the password in the seahub_settings.py to 1.
3. Seafile limits the number of API accesses. For a smooth use of this Zimlet it is recommended to increase the maximum permitted connections in Seafile. Add the following settings to your seahub_settings.py and restart Seafile:

         # API throttling related settings. Enlarger the rates if you got 429 response code during API calls.
         REST_FRAMEWORK = {
             'DEFAULT_THROTTLE_RATES': {
                 'ping': '600/minute',
                 'anon': '30/minute',
                 'user': '5000/minute',
             },
             'UNICODE_JSON': False,
         }

         # Throtting whitelist used to disable throttle for certain IPs.
         # e.g. REST_FRAMEWORK_THROTTING_WHITELIST = ['127.0.0.1', '192.168.1.1']
         REST_FRAMEWORK_THROTTING_WHITELIST = []
      
More details can be found here: https://manual.seafile.com/config/seahub_settings_py.html#restful-api
