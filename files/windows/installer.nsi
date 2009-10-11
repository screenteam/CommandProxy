; Taken from http://nsis.sourceforge.net/Simple_installer_with_JRE_check by weebib
; Use it as you desire.
; Credit given to so many people of the NSIS forum.

!define AppName "${id}"
!define AppVersion "${version}"
!define SetupFile "${setupFile}"
!define ShortName "${filename}"
!define JRE_VERSION "1.6.0"
!define AirFile "${airFile}"
!define JRE_URL "http://javadl.sun.com/webapps/download/AutoDL?BundleId=18714&/jre-6u5-windows-i586-p.exe"
!define AIR_URL "http://airdownload.adobe.com/air/win/download/latest/AdobeAIRInstaller.exe"

; Include the nsDialog things
!include LogicLib.nsh
!include nsDialogs.nsh

!addplugindir "${NSISDIR}\..\nsis_reg\Plugin"
!addincludedir "${NSISDIR}\..\nsis_reg\Include"

!addplugindir "${NSISDIR}\..\nsis_nsunzip\Plugin"

!include "MUI2.nsh"
!include "Sections.nsh"
!include "Registry.nsh"

;--------------------------------
; Variables
Var downloadJRE ; do we need to download the jre? 
Var downloadAIR ; do we need to download adobe air? 
Var cDialog ; The custom dialog showing the required dependencies
Var cListBox ; A listbox listing those depencies

;--------------------------------
;A tiny Language Macro
!macro LANG_LOAD LANGLOAD
	!insertmacro MUI_LANGUAGE "${LANGLOAD}"
	!define L ${LANG_${LANGLOAD}}
	!include "${NSISDIR}\..\nsis_lang\${LANGLOAD}.nsh"
	!undef L
!macroend
 
;--------------------------------
;Configuration

;General
Name "${AppName}"
OutFile "${SetupFile}"
 
;Folder selection page
InstallDir "$PROGRAMFILES\${SHORTNAME}"
 
;Get install folder from registry if available
;InstallDirRegKey HKLM "SOFTWARE\${Vendor}\${ShortName}" ""

;Language
!insertmacro LANG_LOAD "German"

;--------------------------------
; Pages config
!define MUI_PAGE_HEADER_TEXT "${ShortName}"
!define MUI_PAGE_HEADER_SUBTEXT "${AppName}"
!define MUI_FINISHPAGE_TITLE "$(FINISHED_HEAD)"
!define MUI_FINISHPAGE_TEXT "$(FINISHED_TEXT)"
!define MUI_FINISHPAGE_BUTTON "$(FINISHED_BUTTON)"
!define MUI_FINISHPAGE_RUN "$INSTDIR\${ShortName}.exe"
!define MUI_FINISHPAGE_RUN_TEXT "$(FINISHED_RUNTEXT)"
!define MUI_INSTFILESPAGE_FINISHHEADER_TEXT "$(COPY_FINISHED_HEAD)"
!define MUI_INSTFILESPAGE_FINISHHEADER_SUBTEXT "$(COPY_FINISHED_SUBHEAD)"
!define MUI_INSTFILESPAGE_ABORTHEADER_TEXT "$(COPY_ABORT_HEAD)"
!define MUI_INSTFILESPAGE_ABORTHEADER_SUBTEXT "$(COPY_ABORT_SUBHEAD)"

;--------------------------------
;Pages 

Page custom checkDependencies
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

Section "Dependencies"
	Call installDependencies
SectionEnd
 
Section "Files"
	SectionIn 1 RO	; Full install, cannot be unselected
	SetOutPath $INSTDIR
	File "${ShortName}.exe"
	File "/oname=$TEMP\${ShortName}.air" "${AirFile}"
	
	; Is the air-app already installed? 
	Call tweakAirApp
	Pop $R8
	StrCmp $R8 "OK" onlyUnzip
	Goto realInstall
	
	onlyUnzip:
		;ZipDLL::extractall "$TEMP\${ShortName}.air" "$INSTDIR\test"
		nsUnzip::Extract "$TEMP\${ShortName}.air" "/d=$INSTDIR\air\${ShortName}" /END

		;MessageBox MB_OK "unzippi pippi nach $INSTDIR\air\${ShortName}"
		Goto done
		
		
	realInstall: 
	;MessageBox MB_OK "Real install..."
	
	; Now find that airappinstaller.exe
	Call findAirExecutable
	Pop $R0
	nsExec::ExecToStack '"$R0" -silent -location "$INSTDIR\air" "$TEMP\${ShortName}.air"'
	Pop $0

	;MessageBox MB_OK "Result=$0; executable=$R0"
	; Great, it's installed! 
	; Yea... not really, thing is... the air installer seems to fork a new 
	; process, which sucks. So let's give this up to ten tries... 
	StrCpy $R9 ""
	repeat: 
		Sleep 1000
		StrCmp $R9 "XXXXXXXXXX" done
		Call tweakAirApp
		Pop $R8
		StrCmp $R8 "OK" done
		StrCpy $R9 "X$R9"
		Goto repeat
	done: 
	
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${ShortName}" "DisplayName" "${AppName}"
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${ShortName}" "UninstallString" '"$INSTDIR\uninstall.exe"'
	WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${ShortName}" "NoModify" "1"
	WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${ShortName}" "NoRepair" "0"
	
	;Create uninstaller
	WriteUninstaller "$INSTDIR\Uninstall.exe"
SectionEnd
 
 
Section "Shortcuts"
	CreateDirectory "$SMPROGRAMS\${AppName}"
	CreateShortCut "$SMPROGRAMS\${AppName}\${ShortName}.lnk" "$INSTDIR\${ShortName}.exe" "" "$INSTDIR\${ShortName}.exe" 0
	CreateShortCut "$SMPROGRAMS\${AppName}\Uninstall.lnk" "$INSTDIR\uninstall.exe" "" "$INSTDIR\uninstall.exe" 0
SectionEnd


 
Function .onInit
FunctionEnd

Function findAirExecutable
	ReadRegStr $R0 HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\Adobe AIR" "InstallLocation"
	
	FindFirst $0 $R1 "$R0\Versions\*"
	Goto next
	
	next: 
		StrCmp $R1 "" fail
		IfFileExists "$R0\Versions\$R1\Resources\airappinstaller.exe" found
		FindNext $0 $R1
		Goto next
		
	found:
		FindClose $0
		Push "$R0\Versions\$R1\Resources\airappinstaller.exe"
		Return
		
	fail: 
		FindClose $0
		MessageBox MB_OK "$(AIR_BROKEN)"
		Quit
FunctionEnd


Function tweakAirApp
	${registry::Open} "HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall" '/N=InstallLocation /T=REG_KEY' $0
	StrCpy $1 "$INSTDIR\air"
	StrLen $2 $1
	StrCpy $4 "NOK"
	; R0 = path
	; R1 = name
	; R2 = value
	; R3 = type
	next: 
		${registry::Find} "$0" $R0 $R1 $R2 $R3
		StrCmp "$R0" "" done
		StrCpy $3 "$R2" "$2"
		StrCmp "$1" "$3" found
		Goto next
		
	found: 
		WriteRegDWORD HKLM "$R0" "SystemComponent" "1"
		StrCpy $4 "OK"
		Goto next
		
	done: 
	${registry::Close} "$0"
	${registry::Unload}	
	
	Push $4
FunctionEnd


Function checkDependencies
	StrCpy $downloadAIR "1"
	ReadRegStr $0 HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\Adobe AIR" "DisplayVersion"
	IfErrors +2 ; If Air was not found skip the next command. Strange syntax that is.... :/
	StrCpy $downloadAIR "0"
	
	StrCpy $downloadJRE "1"
	Call DetectJRE
	Pop $0
	StrCmp $0 "NOK" +2 ; If Jre was not found skip the next line
	StrCpy $downloadJRE "0"
	
	; the following means (if (!downloadJre && !downloadAir) then  abort) 
	StrCmp $downloadJRE "1" +3
	StrCmp $downloadAIR "1" +2
	Abort
	
	!insertmacro MUI_HEADER_TEXT $(DEPENDENCIES_HEAD) $(DEPENDENCIES_SUBHEAD)
  	nsDialogs::Create 1018
	Pop $cDialog

	${NSD_CreateLabel} 0 110u 100% 30u "$(DEPENDENCIES_TEXT)"
	${NSD_CreateListBox} 0 1u 100% 95u ""
	Pop $cListBox
	
	StrCmp $downloadJRE "0" +2
	${NSD_LB_AddString} $cListBox "Java Runtime Environment"
	
	StrCmp $downloadAir "0" +2
	${NSD_LB_AddString} $cListBox "Adobe Air"
	
	nsDialogs::show	
FunctionEnd


Function installDependencies
	!insertmacro MUI_HEADER_TEXT $(DOWNLOADS_HEAD) $(DOWNLOADS_SUBHEAD)
	
	StrCmp $downloadJRE "0" skipJRE
	StrCpy $2 "$TEMP\Java Runtime Environment.exe"
	nsisdl::download /TRANSLATE2 \
		"$(DL_DOWNLOADING)" "$(DL_CONNECTING)" "$(DL_1SECOND)" "$(DL_1MINUTE)" \
		"$(DL_1HOUR)" "$(DL_SECONDS)" "$(DL_MINUTES)" "$(DL_HOURS)" \
		"$(DL_PROGRESS)" /TIMEOUT=30000 ${JRE_URL} $2 
	Pop $R0 ;Get the return value
	StrCmp $R0 "success" +3
	MessageBox MB_OK "$(JRE_DOWNLOAD_FAIL)"
	Quit
	
	Banner::show /NOUNLOAD "$(JRE_INSTALLING)"
	nsExec::ExecToStack '"$2" /q'
	Banner::destroy
	Delete $2
	
	skipJRE: 

	StrCmp $downloadAIR "0" skipAIR
	StrCpy $2 "$TEMP\Adobe AIR.exe"
	nsisdl::download /TRANSLATE2 \
		"$(DL_DOWNLOADING)" "$(DL_CONNECTING)" "$(DL_1SECOND)" "$(DL_1MINUTE)" \
		"$(DL_1HOUR)" "$(DL_SECONDS)" "$(DL_MINUTES)" "$(DL_HOURS)" \
		"$(DL_PROGRESS)" /TIMEOUT=30000 ${AIR_URL} $2 
	Pop $R0 ;Get the return value
	StrCmp $R0 "success" +3
	MessageBox MB_OK "$(AIR_DOWNLOAD_FAIL)"
	Quit
	
	Banner::show /NOUNLOAD "$(AIR_INSTALLING)"
	nsExec::ExecToStack '"$2"'
	Pop $0
	Banner::destroy
	Delete $2
	
	skipAIR: 
FunctionEnd

Function DetectJRE
	ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
	StrCmp $R0 "" DetectTry2
	ReadRegStr $R1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$R0" "JavaHome"
	StrCmp $R1 "" DetectTry2
	Goto GetJRE
	 
	DetectTry2:
		ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Development Kit" "CurrentVersion"
		StrCmp $R0 "" NoFound
		ReadRegStr $R1 HKLM "SOFTWARE\JavaSoft\Java Development Kit\$R0" "JavaHome"
		StrCmp $R1 "" NoFound
	 
	GetJRE:
		IfFileExists "$R1\bin\java.exe" 0 NoFound
		StrCpy $R2 $R0 1
		StrCpy $R3 ${JRE_VERSION} 1
		IntCmp $R2 $R3 0 FoundOld FoundNew
		StrCpy $R2 $R0 1 2
		StrCpy $R3 ${JRE_VERSION} 1 2
		IntCmp $R2 $R3 FoundNew FoundOld FoundNew
	 
	NoFound:
		Push "None"
		Push "NOK"
		Return
	 
	FoundOld:
		Push $R0
		Push "NOK"
		Return
	 
	FoundNew:
		Push "$R1\bin\java.exe"
		Push "OK"
		Return
	 
FunctionEnd

Section "Uninstall"
	; uninstall all the air packages we installed
	${registry::Open} "HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall" '/N=InstallLocation /T=REG_KEY' $0
	StrCpy $1 "$INSTDIR\air"
	StrLen $2 $1

	;MessageBox MB_OK "uninstall..."
	
	next: 
		${registry::Find} "$0" $R0 $R1 $R2 $R3
		StrCmp "$R0" "" done
		;MessageBox MB_OK "$1 == $3?..."
		StrCpy $3 "$R2" "$2"
		StrCmp "$1" "$3" found
		Goto next
		
	found: 
		;MessageBox MB_OK "Read $RO / $R1?..."
		
		ReadRegStr $R1 HKLM "$R0" "UninstallString"
		nsExec::ExecToStack "$R1 /quiet /qn"
		Goto next
		
	done: 
	;MessageBox MB_OK "dahan!"
	
	${registry::Close} "$0"
	${registry::Unload}	
	
	; remove registry keys
	DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${ShortName}"
	;Delete "$SMPROGRAMS\${AppName}"
	RMDir /r "$SMPROGRAMS\${AppName}"
	RMDir /r "$INSTDIR"
SectionEnd