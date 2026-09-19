// Project-local adaptation of the MIT-licensed SPEAR EARS validator; see UPSTREAM-NOTICE.md.
import fs from 'node:fs';
import path from 'node:path';
import {pathToFileURL} from 'node:url';
export function validate(text, filename) {
  const errors=[]; let pending=null;
  const fail=(entry,reason)=>errors.push({id:entry.id,file:filename,line:entry.line,reason});
  const check=(entry,clause)=>{
    const clean=clause.replace(/^\*\*(Ubiquitous|Event-driven|State-driven|Unwanted|Feature)\.\*\*\s*/,'').trim();
    if(!/^(?:THE SYSTEM SHALL\s+\S|WHEN\s+.+\s+THE SYSTEM SHALL\s+\S|WHILE\s+.+\s+THE SYSTEM SHALL\s+\S|IF\s+.+\s+(?:THEN\s+)?THE SYSTEM SHALL\s+\S|WHERE\s+\S)/.test(clean))
      fail(entry,'Clause does not match a supported EARS pattern or has an empty response');
  };
  text.split(/\r?\n/).forEach((line,index)=>{
    const header=line.match(/^###\s+(REQ-\d+)\b/);
    const inline=line.match(/^\s*-\s+(REQ-\d+):\s*(.*)$/);
    if(header || inline) {
      if(pending) fail(pending,'Requirement is missing its EARS clause');
      const entry={id:(header || inline)[1],line:index+1};
      if(inline) {check(entry,inline[2]); pending=null;} else pending=entry;
      return;
    }
    if(pending && line.trim() && !line.startsWith('#')) {check(pending,line.trim());pending=null;}
  });
  if(pending) fail(pending,'Requirement is missing its EARS clause');
  return {ok:errors.length===0,errors};
}
if(process.argv[1] && import.meta.url===pathToFileURL(path.resolve(process.argv[1])).href) {
  try {
    if(!process.argv[2]) throw Error('Usage: node ears.mjs <path>');
    const result=validate(fs.readFileSync(process.argv[2],'utf8'),process.argv[2]);
    for(const e of result.errors) console.error(e.file+':'+e.line+': '+e.id+': '+e.reason);
    process.exitCode=result.ok?0:1;
  } catch(e) {console.error(e.message);process.exitCode=1;}
}
