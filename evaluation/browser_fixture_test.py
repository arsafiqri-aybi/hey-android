#!/usr/bin/env python3
"""Deterministic DOM-only Chromium smoke tests for Hey browser assets.

Requires: pip install playwright, plus a local chromium binary.
Usage: HEY_CHROMIUM=/usr/bin/chromium python3 evaluation/browser_fixture_test.py
This is NOT a substitute for an Android/Vivo WebView or screenshot test.
"""
import json
import os
from pathlib import Path
from playwright.sync_api import sync_playwright

assets=Path(__file__).resolve().parents[1]/'app/src/main/assets'
semantic=(assets/'semantic.js').read_text()
locator=(assets/'locator.js').read_text()
actionability=(assets/'actionability.js').read_text()

def quote(value):return json.dumps(value,ensure_ascii=False)
def locate(page,payload):
    js=locator.replace('HEY_NAMESPACE',quote('hey_fixture')).replace('HEY_QUERY',quote(quote(payload)))
    return json.loads(page.evaluate(js))
def point(page):
    js=actionability.replace('HEY_TARGET',quote('hey_target')).replace('HEY_WEB_WIDTH','720').replace('HEY_NAMESPACE',quote('hey_fixture'))
    return json.loads(page.evaluate(js))

with sync_playwright() as playwright:
    browser=playwright.chromium.launch(executable_path=os.environ.get('HEY_CHROMIUM','/usr/bin/chromium'),headless=True,args=['--no-sandbox'])
    page=browser.new_page(viewport={'width':720,'height':900})
    page.set_content('''<style>#nested{height:160px;width:350px;overflow-y:auto;border:1px solid}#space{height:1200px}#cover{display:none;position:fixed;z-index:999;inset:0;background:#3333}</style>
    <label for="email">Alamat Email</label><input id="email" placeholder="Email pribadi" />
    <label for="secret">Kata sandi</label><input id="secret" type="password" value="dummy-do-not-disclose"/>
    <button id="save" data-testid="save-btn">Simpan</button><button id="other">Batalkan</button>
    <div id="nested"><div id="space"></div><button id="far">Lanjutkan</button></div>
    <div id="cover"></div><div hidden><button>Hidden</button></div>''')
    page.evaluate(semantic.replace('HEY_NAMESPACE',quote('hey_fixture')))
    page.evaluate('window.hey_fixture.refs={};window.hey_fixture.nextRef=0')
    found=locate(page,{'by':'role','query':'button','name':'Simpan'})
    assert found['ref']=='l1' and found['locatorUnique'] and found['role']=='button', found
    assert locate(page,{'by':'label','query':'Alamat Email'})['tag']=='input'
    assert locate(page,{'by':'placeholder','query':'Email pribadi'})['tag']=='input'
    assert locate(page,{'by':'testId','query':'save-btn'})['tag']=='button'
    assert locate(page,{'by':'text','query':'Batalkan'})['tag']=='button'
    assert locate(page,{'by':'css','query':'#save'})['tag']=='button'
    assert locate(page,{'by':'role','query':'button','name':'Sim', 'exact':False})['tag']=='button'
    protected=locate(page,{'by':'label','query':'Kata sandi'})
    assert protected['sensitive'] and protected['label']=='[sensitive]' and 'dummy-do-not-disclose' not in str(protected)
    assert locate(page,{'by':'role','query':'button'})['error']=='MULTIPLE_MATCHES'
    assert locate(page,{'by':'role','query':'button','name':'Not Here'})['error']=='ELEMENT_NOT_FOUND'
    assert locate(page,{'by':'role','query':'button','name':'Hidden'})['error']=='ELEMENT_NOT_FOUND'
    assert locate(page,{'by':'css','query':'['})['error']=='INVALID_LOCATOR_QUERY'
    # JSON-stringified query never becomes executable webpage JavaScript.
    assert locate(page,{'by':'text','query':'");window.hey_injected=true;//'})['error']=='ELEMENT_NOT_FOUND'
    assert not page.evaluate('window.hey_injected===true')
    far=locate(page,{'by':'role','query':'button','name':'Lanjutkan'})
    assert far['locatorUnique']
    page.evaluate('(ref)=>window.hey_target=window.hey_fixture.refs[ref]',far['ref'])
    a=point(page);b=point(page)
    assert a.get('error') is None and b.get('error') is None,(a,b)
    assert page.evaluate("document.querySelector('#nested').scrollTop")>700
    page.evaluate("document.querySelector('#cover').style.display='block'")
    assert point(page)['error']=='ELEMENT_OBSCURED'
    page.evaluate("document.querySelector('#cover').style.display='none';document.querySelector('#far').disabled=true")
    assert point(page)['error']=='ELEMENT_DISABLED'
    page.evaluate("document.querySelector('#far').remove()")
    assert point(page)['error']=='STALE_REFERENCE'
    browser.close()
print('PASS: semantic selectors, secret-redacted locator result, ambiguous/missing/hidden/invalid, injection inert, nested scroll, overlay, disabled, stale')
