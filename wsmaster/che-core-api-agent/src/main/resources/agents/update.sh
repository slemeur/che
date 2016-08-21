#!/bin/bash
updateAgentScript() {
  DIR=$1
  AGENT=$2
  SCRIPT=$(cat $DIR/$AGENT.script.sh | sed -r 's/"/\\"/g' | sed -r ':a;N;$!ba;s/\n/\\n/g')

  touch $AGENT.json.tmp    
  cat $DIR/$AGENT.json | while read line
  do
    if echo $line | grep -qi "script"; then 
      echo \"script\" : \"$SCRIPT\" >> $AGENT.json.tmp
    else
      echo $line >> $AGENT.json.tmp
    fi
  done

  mv $AGENT.json.tmp $DIR/$AGENT.json
}

updateAgentScript "." "org.eclipse.che.ssh"
updateAgentScript "." "org.eclipse.che.terminal"
updateAgentScript "." "org.eclipse.che.ws-agent"




