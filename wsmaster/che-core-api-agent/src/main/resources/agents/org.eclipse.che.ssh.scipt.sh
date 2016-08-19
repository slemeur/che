pidof sshd && exit

shopt -s nocasematch
LINUX_TYPE=`cat /etc/os-release | grep ^ID=`

###############################
### Install Needed packaged ###
###############################

# Red Hat Enterprise Linux 7 
############################
if echo $LINUX_TYPE | grep -qi "rhel"; then
    sudo yum -y install openssh-server
    sudo mkdir -p /var/run/sshd
    sudo sed 's@session\s*required\s*pam_loginuid.so@session optional pam_loginuid.so@g' -i /etc/pam.d/sshd
    sudo /usr/bin/ssh-keygen -A && sudo /usr/sbin/sshd -D 

# Ubuntu 14.04 16.04 / Linux Mint 17 
####################################
elif echo $LINUX_TYPE | grep -qi "ubuntu"; then
    sudo apt-get update
    sudo apt-get -y install openssh-server
    sudo mkdir -p /var/run/sshd
    sudo sed 's@session\s*required\s*pam_loginuid.so@session optional pam_loginuid.so@g' -i /etc/pam.d/sshd
    sudo /usr/bin/ssh-keygen -A && sudo /usr/sbin/sshd -D 

# Debian 8
##########
elif echo $LINUX_TYPE | grep -qi "debian"; then
    sudo apt-get update
    sudo apt-get -y install openssh-server
    sudo mkdir -p /var/run/sshd
    sudo sed 's@session\s*required\s*pam_loginuid.so@session optional pam_loginuid.so@g' -i /etc/pam.d/sshd
    sudo /usr/bin/ssh-keygen -A && sudo /usr/sbin/sshd -D 

# Fedora 23
###########
elif echo $LINUX_TYPE | grep -qi "fedora"; then
    sudo yum -y install openssh-server
    sudo mkdir -p /var/run/sshd
    sudo sed 's@session\s*required\s*pam_loginuid.so@session optional pam_loginuid.so@g' -i /etc/pam.d/sshd
    sudo /usr/bin/ssh-keygen -A && sudo /usr/sbin/sshd -D 

# CentOS 7.1 & Oracle Linux 7.1
###############################
elif echo $LINUX_TYPE | grep -qi "centos"; then
    sudo yum -y install openssh-server
    sudo mkdir -p /var/run/sshd
    sudo sed 's@session\s*required\s*pam_loginuid.so@session optional pam_loginuid.so@g' -i /etc/pam.d/sshd
    sudo /usr/bin/ssh-keygen -A && sudo /usr/sbin/sshd -D 

# openSUSE 13.2
###############
elif echo $LINUX_TYPE | grep -qi "opensuse"; then
    sudo zypper install -y openSSH
    sudo mkdir -p /var/run/sshd
    sudo sed 's@session\s*required\s*pam_loginuid.so@session optional pam_loginuid.so@g' -i /etc/pam.d/sshd
    sudo /usr/bin/ssh-keygen -A && sudo /usr/sbin/sshd -D 

# Alpine 3.3
############$$
elif echo $LINUX_TYPE | grep -qi "alpine"; then
    sudo apk update
    sudo apk add openssh
    sudo mkdir -p /var/run/sshd
    sudo /usr/bin/ssh-keygen -A && sudo /usr/sbin/sshd -D 

else
    >&2 echo "Unrecognized Linux Type"
    >&2 cat /etc/os-release
    exit 1
fi
