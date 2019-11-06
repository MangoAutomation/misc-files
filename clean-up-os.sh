#!/bin/bash

set -e

rm -f /home/ubuntu/.bash_history
rm -f /home/ubuntu/.mysql_history
rm -f /home/ubuntu/.viminfo
rm -rf /home/ubuntu/.vim

rm -f ~/.bash_history
rm -f ~/.mysql_history
rm -f ~/.viminfo
rm -rf ~/.vim
find /var/log -type f -name "*.gz" -delete
find /var/log -type f -name "*.[1-9]" -delete

apt-get clean
apt-get autoclean
