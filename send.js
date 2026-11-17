(function(){
"use strict";
var $=function(s){return document.querySelector(s)};
var h=location.hash.replace(/^#/,""), p=new URLSearchParams(h);
var to=(p.get("t")||"").replace(/\D/g,""), body=p.get("b")||"";
// 주소창·기록에 문자 내용이 남지 않게 해시를 지운다
try{history.replaceState(null,"",location.pathname)}catch(e){}
if(to&&(to.length<9||to.length>12)) to="";
if(!body||body.length>4000){$("#empty").hidden=false;return}
var ios=/iPhone|iPad|iPod/i.test(navigator.userAgent);
function fmt(d){return d.length===11?d.slice(0,3)+"-"+d.slice(3,7)+"-"+d.slice(7):d.length===10?d.slice(0,3)+"-"+d.slice(3,6)+"-"+d.slice(6):d}
$("#ready").hidden=false;
$("#num").textContent=to?fmt(to):"(번호 없음 · 문자앱에서 직접 선택)";
$("#body").textContent=body;
$("#open").setAttribute("href","sms:"+to+(ios?"&":"?")+"body="+encodeURIComponent(body));
function copy(t,btn){
  var done=function(){var o=btn.textContent;btn.textContent="복사했어요";setTimeout(function(){btn.textContent=o},1500)};
  if(navigator.clipboard&&navigator.clipboard.writeText){navigator.clipboard.writeText(t).then(done,function(){fallback()})}else fallback();
  function fallback(){var a=document.createElement("textarea");a.value=t;document.body.appendChild(a);a.select();try{document.execCommand("copy");done()}catch(e){}a.remove()}
}
$("#cpBody").addEventListener("click",function(){copy(body,this)});
$("#cpNum").addEventListener("click",function(){copy(to,this)});
if(!to) $("#cpNum").hidden=true;
$("#note").textContent="문자앱이 열리지 않으면 내용 복사와 번호 복사를 눌러 문자앱에 붙여넣어 주세요.";
})();
