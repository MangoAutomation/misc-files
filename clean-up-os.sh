#!/bin/bash

set -e

rm -f /home/ubuntu/.bash_history
rm -f /home/ubuntu/.bash_history
rm -f /home/ubuntu/.viminfo
rm -rf /home/ubuntu/.vim

rm -f ~/.bash_history
rm -f ~/.mysql_history
#rm -f /etc/machine-id
rm -f /var/log/**/*.{1..9}
rm -f /var/log/**/*.gz

apt-get clean
apt-get autoclean
