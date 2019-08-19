# Seafile Zimlet
Here you can find the installer and sources for the Seafile modification of the Zimbra WebDAV Client/Nextcloud Zimlet. For more information refer to the readme here: https://github.com/Zimbra-Community/owncloud-zimlet


### Installing
Choose Y when asked to install OCS.

    wget --no-cache https://raw.githubusercontent.com/Zimbra-Community/owncloud-zimlet/soapServiceBarry/webdav-client-installer.sh -O /tmp/webdav-client-installer.sh
    chmod +rx /tmp/webdav-client-installer.sh
    /tmp/webdav-client-installer.sh 

### Known issues

1. UI allows saving to root folders (Libraries) but this does not work in Seafile. No error is displayed in case the user saves attachments to the root, but they won't succeed.
2. Seafile has a minimum length for sharing passwords. But there is no check on this in the Zimlet. (https://manual.seafile.com/config/seahub_settings_py.html; SHARE_LINK_PASSWORD_MIN_LENGTH = 8). If the password is to short there is an error.
You can either leave it as is, and confront your users with `Cannot create share` error or set the minimum length of the password in the seahub_settings.py to 1.
