Summary: None
Name: None
Version: None
Release: None
Source0: %{name}-%{version}-%{release}-bin.tar.gz 
License: None
Group: None
BuildRoot: %_topdir/BUILD/%{name}-root

%description
None



%preun
#if [ -e /etc/init.d/dataconnector ] ; then 
#  /etc/init.d/dataconnector stop
# else
# echo "nothinng to do"
#fi

#if [ -e /etc/opt/dataconnector/rules.properties ] ; then 
# echo "Backing up existing rules.properties" 
# cp -f /etc/opt/dataconnector/rules.properties /etc/opt/dataconnector/rules.properties.rpm.uninstall.$RANDOM
#fi


%pre 
#if [ -e /etc/opt/dataconnector/rules.properties ] ; then
# echo "Existing rules.propertiess. Will keep existing file." 
# cp -f /etc/opt/dataconnector/rules.properties /etc/opt/dataconnector/rules.properties.pre
#fi

%prep 
#%setup -c -n %{name}-%{version} 

%build
#ant binary-distro 

%install
rm -rf $RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT/opt/%{name}
mkdir -p $RPM_BUILD_ROOT/etc/opt/%{name}
tar -vxzf $RPM_SOURCE_DIR/%{name}-%{version}-%{release}-bin.tar.gz -C $RPM_BUILD_ROOT/opt/%{name}
cp -f $RPM_BUILD_ROOT/opt/%{name}/third-party/openssh/dist/sshd_config.tmpl $RPM_BUILD_ROOT/etc/opt/%{name}/sshd_config
mv $RPM_BUILD_ROOT/opt/%{name}/config/* $RPM_BUILD_ROOT/etc/opt/%{name}
mv $RPM_BUILD_ROOT/etc/opt/%{name}/localConfig.xml-dist $RPM_BUILD_ROOT/etc/opt/%{name}/localConfig.xml
mv $RPM_BUILD_ROOT/etc/opt/%{name}/resourceRules.xml-dist $RPM_BUILD_ROOT/etc/opt/%{name}/resourceRules.xml
mv $RPM_BUILD_ROOT/etc/opt/%{name}/httpd.conf-dist $RPM_BUILD_ROOT/etc/opt/%{name}/httpd.conf

#rm -rf $RPM_BUILD_ROOT/opt/%{name}/config


%post 
set -e
OPENSSH_ROOT="/opt/dataconnector/third-party/openssh"
WOODSTOCK_HOME=${OPENSSH_ROOT}"/home/woodstock"
LOGFILE="/var/log/dataconnector"
RULES_DIR="/var/opt/dataconnector"

mkdir -p $OPENSSH_ROOT/bin
cp -f $OPENSSH_ROOT/dist/start_sshd.sh.tmpl $OPENSSH_ROOT/bin/start_sshd.sh
rm -rf /opt/dataconnector/third-party/openssh/dist

# Setup the user to run the client
adduser -s /bin/false -U -M dataconnector 
# Set up the user to access the client
adduser -s /bin/false -U -d $WOODSTOCK_HOME -M \
woodstock

dd if=/dev/urandom bs=1024 count=1 |passwd --stdin woodstock

if [ ! -e $LOGFILE ]; then
touch $LOGFILE
fi
chown root:dataconnector $LOGFILE
chmod 660 $LOGFILE

if [ ! -d $RULES_DIR ]; then
mkdir -p $RULES_DIR
chmod 700 $RULES_DIR
fi

if [ ! -f $RULES_DIR/rules ]; then
touch $RULES_DIR/rules
fi
chmod 0600 $RULES_DIR/rules
chown root:root $RULES_DIR/rules

chmod 770 /etc/opt/dataconnector
chown -R root:dataconnector /etc/opt/dataconnector
chown -R root:dataconnector /opt/dataconnector

chmod 750 $OPENSSH_ROOT/bin/start_sshd.sh
chmod 750 $OPENSSH_ROOT/sbin/sshd
 

if [ -e /etc/opt/dataconnector/rules.properties.pre ] ; then 
 mv /etc/opt/dataconnector/rules.properties /etc/opt/dataconnector/rules.properties.rpm
fi

if [ -e /etc/opt/dataconnector/rules.properties.preinstall ] ; then
   mv /etc/opt/dataconnector/rules.properties.preinstall /etc/opt/dataconnector/rules.properties
fi

ln -fs /opt/dataconnector/third-party/java-service-wrapper/linux/dataconector.shh /etc/init.d/dataconnector
 
%postun
echo "Removing User Accounts.."

userdel -r woodstock || true
userdel -r dataconnector || true

if [ -e /opt/dataconnector ] ; then
 rm -rf /opt/dataconnector
fi

if [ -e /etc/init.d/dataconnector ] ; then
 rm -f /etc/init.d/dataconnector
fi

%clean                               
rm -rf $RPM_BUILD_ROOT

%files
%defattr(-,root,root) 
%config /etc/opt/%{name}/httpd.conf
%config /etc/opt/%{name}/resourceRules.xml
%config /etc/opt/%{name}/localConfig.xml
/etc/opt/%{name}/* 
/opt/%{name}/*  



