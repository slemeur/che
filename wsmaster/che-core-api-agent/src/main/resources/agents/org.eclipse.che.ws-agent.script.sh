BASE_DIR=$HOME/che
LINUX_TYPE=`cat /etc/os-release | grep ^ID=`
LINUX_VERSION=`cat /etc/os-release | grep ^VERSION=`

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


mkdir -p $BASE_DIR
sudo mkdir -p /projects
sudo sh -c "chown -R $(id -u -n) /projects"

####################
### Install JAVA ###
####################

JRE_URL=http://download.oracle.com/otn-pub/java/jdk/8u45-b14/jre-8u45-linux-x64.tar.gz
wget -qO - --no-cookies --no-check-certificate --header "Cookie: oraclelicense=accept-securebackup-cookie" "${JRE_URL}" | tar -C $BASE_DIR -xzf -

mkdir -p $BASE_DIR/ws-agent
wget  -qO - https://codenvy.com/update/repository/public/download/org.eclipse.che.ws-agent.binaries | tar -C $BASE_DIR/ws-agent -xzf -

JPDA_ADDRESS=4403 && $BASE_DIR/ws-agent/bin/catalina.sh jpda run
