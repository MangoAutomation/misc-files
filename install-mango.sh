#!/bin/bash

set -e
mango_version="$1"

systemctl stop mango || true
systemctl disable mango || true

mysql -u root < ~/drop-mango.sql

rm -rf /opt/mango/*
rm -f /opt/mango/.ma
wget -O /tmp/mango.zip https://store.infiniteautomation.com/downloads/fullCores/enterprise-m2m2-core-"$mango_version".zip
unzip /tmp/mango.zip -d /opt/mango
rm -f /tmp/mango.zip

cp ~/env.properties /opt/mango/overrides/properties/
echo "ssl.keystore.password=$(openssl rand -base64 24)" >> /opt/mango/overrides/properties/env.properties

get-script() {
	wget -O "/opt/mango/bin/$1" "https://raw.githubusercontent.com/infiniteautomation/ma-core-public/main/Core/scripts/$1"
}

get-script ma-start-systemd.sh
get-script mango.service
get-script getenv.sh
get-script genkey.sh
get-script certbot-deploy.sh

chmod +x /opt/mango/bin/*.sh

/opt/mango/bin/genkey.sh

chown -R mango:mango /opt/mango
rm -f /etc/systemd/system/mango.service
ln -s /opt/mango/bin/mango.service /etc/systemd/system/mango.service
systemctl enable mango

