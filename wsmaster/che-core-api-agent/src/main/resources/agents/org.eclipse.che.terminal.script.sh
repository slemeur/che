pidof che-websocket-terminal && exit

BASE_DIR=$HOME/che
mkdir -p $BASE_DIR
LINUX_TYPE=`cat /etc/os-release | grep ^ID=`
LINUX_VERSION=`cat /etc/os-release | grep ^VERSION=`
MACHINE_TYPE=`uname -m`

shopt -s nocasematch

###############################
### Install Needed packaged ###
###############################

# Red Hat Enterprise Linux 7 
############################
if echo $LINUX_TYPE | grep -qi "rhel"; then
    sudo yum install tar wget

# Ubuntu 14.04 16.04 / Linux Mint 17 
####################################
elif echo $LINUX_TYPE | grep -qi "ubuntu"; then
    sudo apt-get update
    sudo apt-get -y install tar wget

# Debian 8
##########
elif echo $LINUX_TYPE | grep -qi "debian"; then
    sudo apt-get update
    sudo apt-get -y install tar wget

# Fedora 23 
###########
elif echo $LINUX_TYPE | grep -qi "fedora"; then
    sudo yum -y install tar wget

# CentOS 7.1 & Oracle Linux 7.1
###############################
elif echo $LINUX_TYPE | grep -qi "centos"; then
    sudo yum -y install tar wget

# openSUSE 13.2
###############
elif echo $LINUX_TYPE | grep -qi "opensuse"; then
    sudo zypper install -y tar wget

# Alpine 3.3
############$$
elif echo $LINUX_TYPE | grep -qi "alpine"; then
    sudo apk update
    sudo apk add tar wget

else
    >&2 echo "Unrecognized Linux Type"
    >&2 cat /etc/os-release
    exit 1
fi

########################
### Install Terminal ###
########################
if echo $MACHINE_TYPE | grep -qi "x86_64"; then
    PREFIX=linux_amd64
elif echo $MACHINE_TYPE | grep -qi "arm5"; then
    PREFIX=linux_arm7
elif echo $MACHINE_TYPE | grep -qi "arm6"; then
    PREFIX=linux_arm7
elif echo $MACHINE_TYPE | grep -qi "arm7"; then
    PREFIX=linux_arm7
else
    >&2 echo "Unrecognized Machine Type"
    >&2 uname -a
    exit 1
fi

wget  -qO - https://codenvy.com/update/repository/public/download/org.eclipse.che.terminal.binaries.$PREFIX | tar -C $BASE_DIR -xzf -
$HOME/che/terminal/che-websocket-terminal -addr :4411 -cmd /bin/bash -static $HOME/che/terminal/
