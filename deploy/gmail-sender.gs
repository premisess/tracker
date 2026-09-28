// FitTracker email sender: lets the app send email through this Gmail account over HTTPS,
// for hosts that block SMTP (Render's free plan). No extra service or phone check needed.
//
// 1. Signed in as fittrackers2026@gmail.com, open https://script.google.com and click New project.
// 2. Replace everything in Code.gs with this file, and put your own long random text in SECRET.
// 3. Deploy > New deployment > type: Web app. Execute as: Me. Who has access: Anyone. Deploy,
//    and allow the permissions it asks for (Advanced > Go to project if Google warns).
// 4. Copy the Web app URL. In Render, set MAIL_SCRIPT_URL to that URL and MAIL_SCRIPT_SECRET to
//    the same text as SECRET below.
//
// Gmail allows about 100 emails a day this way on a normal account.

const SECRET = 'PUT-THE-SAME-SECRET-AS-MAIL_SCRIPT_SECRET-HERE';

function doPost(e) {
  const data = JSON.parse(e.postData.contents);
  if (data.secret !== SECRET) {
    return ContentService.createTextOutput('forbidden');
  }
  GmailApp.sendEmail(data.to, data.subject, data.text, {
    name: 'FitTracker',
    replyTo: data.replyTo,
  });
  return ContentService.createTextOutput('ok');
}
