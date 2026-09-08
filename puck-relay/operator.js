const fs=require('node:fs');
const path=require('node:path');
async function main(){
 const [action,id,value]=process.argv.slice(2);
 const token=fs.readFileSync(path.join(__dirname,'.runtime/operator-token'),'utf8');
 let url='/jobs',method='GET',body;
 if(action==='claim'){url=`/jobs/${id}/claim`;method='POST';body={conversationUrl:value};}
 else if(action==='reply'){url=`/jobs/${id}/reply`;method='POST';body={text:fs.readFileSync(value,'utf8')};}
 else if(action!=='pending')throw Error('Usage: node operator.js pending | claim TURN_ID CONVERSATION_URL | reply TURN_ID TEXT_FILE');
 const response=await fetch(`http://127.0.0.1:${process.env.PUCK_OPERATOR_PORT||8790}${url}`,{method,headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},...(body?{body:JSON.stringify(body)}:{})});
 const result=await response.json();if(!response.ok)throw Error(result.error||`HTTP ${response.status}`);
 console.log(JSON.stringify(result,null,2));
}
main().catch(e=>{console.error(e.message);process.exitCode=1;});
