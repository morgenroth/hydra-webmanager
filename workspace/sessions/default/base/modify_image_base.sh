#!/bin/bash
#
# This is executed in a chroot inside the mounted image by prepare_image_base.sh
#
# This file is for the base setup, i.e. it will be excuted only ONCE and all
# changes done  by this file are copied to all nodes
#
# This can be empty
#

echo "Modify base image"

# disable all unnecessary services
if [ -x /etc/init.d/httpd ]; then
	/etc/init.d/httpd disable
fi

if [ -x /etc/init.d/dnsmasq ]; then
	/etc/init.d/dnsmasq disable
fi

if [ -x /etc/init.d/firewall ]; then
	/etc/init.d/firewall enable
fi

if [ -x /etc/init.d/ibrdtn ]; then
	/etc/init.d/ibrdtn enable
fi

echo "Modify base image done."

