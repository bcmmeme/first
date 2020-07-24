version: 0.1
tasks: 
  - type: command
    name: before install
   # command: "chmod +x /home1/irteam/deploy/i.WebConsole.v2_tcdeploy/target/before_install.sh; /home1/irteam/deploy/i.WebConsole.v2_tcdeploy/target/before_install.sh $${serverGroup.name}"
    command: "mkdir -p /home/ubuntu/dep"
    user: ubuntu
#  - type: command
#    name: before install
#    command: "/home1/irteam/apps/nginx/sbin/nginx -s stop" 
#    user: irteam
#  - type: binary
#    user: irteam
#    targetDir: /home1/irteam/deploy
#  - type: command
#    name: unzip
#    command: "mkdir -p /home1/irteam/deploy/i.WebConsole.v2_tcdeploy; rm -rf /home1/irteam/deploy/i.WebConsole.v2_tcdeploy/*; unzip -qq -o /home1/irteam/deploy/app.zip -d /home1/irteam/deploy/i.WebConsole.v2_tcdeploy/"
#    user: irteam
#  - type: command
#    name: after install
#    command: "chmod +x /home1/irteam/deploy/i.WebConsole.v2_tcdeploy/target/after_install.sh; /home1/irteam/deploy/i.WebConsole.v2_tcdeploy/target/after_install.sh $${serverGroup.name}"
#    user: irteam
#  - type: command
#    name: start nginx
#    command: "/home1/irteam/apps/nginx/sbin/nginx"
#    user: irteam
#  - type: command
#    name: log rotate
#    command: "sudo chown root:root /home1/irteam/apps/nginx/conf/nginx.iaas-web-console.logrotate"
#   user: irteamsu
