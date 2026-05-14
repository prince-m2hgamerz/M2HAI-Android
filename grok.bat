@echo off
title Gemini API Tester

set GEMINI_API_KEY=AIzaSyB2W1hw2MGtRmwJ9b1XERO0R1b7fyJ1VWs

echo ========================================
echo Sending Test Message...
echo ========================================

timeout /t 5 >nul

powershell -ExecutionPolicy Bypass -Command "$body=@{ contents=@(@{ parts=@(@{ text='Hello Gemini API test successful' }) }) } | ConvertTo-Json -Depth 10; $url='https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-lite:generateContent?key=%GEMINI_API_KEY%'; try { $response=Invoke-RestMethod -Uri $url -Method Post -ContentType 'application/json' -Body $body; $response.candidates.content.parts.text } catch { Write-Host $_.Exception.Message }"

pause