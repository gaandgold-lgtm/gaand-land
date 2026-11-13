(function(){
var ua=navigator.userAgent||'',and=/Android/i.test(ua);
var kakao=/KAKAOTALK/i.test(ua),insta=/Instagram|FBAN|FBAV/i.test(ua);
if(!kakao&&!(insta&&and)){window.GAAND_EXT=function(){return ''};return}
function ext(u){
  if(kakao)return 'kakaotalk://web/openExternal?url='+encodeURIComponent(u);
  return 'intent://'+u.replace(/^https?:\/\//,'')+'#Intent;scheme=https;action=android.intent.action.VIEW;category=android.intent.category.BROWSABLE;end';
}
window.GAAND_EXT=ext;
var here=location.href.split('#')[0];
document.addEventListener('click',function(e){
  var a=e.target.closest&&e.target.closest('a[href^="sms:"]');
  if(!a)return;e.preventDefault();location.href=ext(here);
},true);
if(document.documentElement.getAttribute('data-autoext')==='1'){
  var k='gaandExt:'+location.pathname,done=false;
  try{done=!!sessionStorage.getItem(k);sessionStorage.setItem(k,'1')}catch(e){}
  if(!done)location.href=ext(here);
}
})();
