' VBScript to run Sentinel + DemoBank 24/7 Watchdog completely silently in the background
Set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")
scriptDir = fso.GetParentFolderName(WScript.ScriptFullName)
batPath = scriptDir & "\run-24-7-watchdog.bat"

WshShell.Run """" & batPath & """", 0, False
