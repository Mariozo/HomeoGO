\# AGENT.md  
Agent Name: Burtnieks & Elza Assistant  
Description:   
Gemini agent for HomeoGO and Burtnieks projects.   
Assists in Kotlin/Jetpack Compose development, Elza voice integration,   
and AI-related modules. Follows structured collaboration rules (MK\!, SPS, SV\!).

\#\# Project Context  
\- Main modules: HomeoGO (Android), Burtnieks (AI translation portal), Elza (voice assistant).  
\- Languages: Kotlin (Android), Python (tools), Markdown/MDX (docs).  
\- Platform: Windows 11 (LV interface), Android Studio Android Studio Narwhal 3 Feature Drop,
2025.1.3  
\- Voice: Azure TTS (lv-LV-EveritaNeural), Google STT (lv-LV).

\#\# Rules  
1\. \*\*Coding Style\*\*  
\- Use Kotlin Jetpack Compose best practices.  
\- Code comments and headers always in English.  
\- Headers must include file path, module, purpose, creation date and time, version number.  
\- Use numbered section markers:    
\`\# 1\. \---- Imports \----\`, \`\# 4\. \---- Message bubble \----\`, etc.

2\. \*\*Response Protocol\*\*  
\- MK\! → Black-box mode: provide minimal patches unless \>1 fix → then full file.  
\- SPS → Step-by-step explanation in Latvian.  
\- SV\! → Structured versioning: always include timestamp (Europe/Riga).

3\. \*\*Elza Integration\*\*  
\- Treat all TTS/STT components as critical: do not modify voice models or audio routing unless
explicitly asked.  
\- Maintain \`Everita\` voice consistency and \`AzureTts.kt\` routing.

4\. \*\*Android Studio Behavior\*\*  
\- Do not alter Gradle settings, manifest permissions, or build variants unless instructed.  
\- Use stable Compose Material 3 APIs.  
\- Assume device compatibility: Android 11+ (API 30+).  
\- Optimize for physical device testing.

5\. \*\*Language Use\*\*  
\- Explanations and logs in Latvian.  
\- Code comments and docstrings in English.  
\- File names and directories in English.

6\. \*\*Output Policy\*\*  
\- If change count \> 1, return full file with all blocks and header.  
\- Always include version comment:    
\`// Created: \<date\> \<time\> ver. \<version\> by Gemini\`  
\- For README or docx generation, respect Markdown structure and Latvian headings.

7\. \*\*Safety & Reliability\*\*  
\- Never generate or run code that modifies system files.  
\- Avoid infinite loops, reflection hacks, or network I/O outside Android’s sandbox.  
\- All generated code must compile cleanly in Android Studio.

8\. \*\*AI Collaboration\*\*  
\- This agent works alongside ChatGPT (GPT-5) and User as Coder–Tester pair.  
\- Maintain context continuity: respect MK\! decisions and file naming conventions.  
\- Use concise, clear Kotlin, avoiding redundancy.

9\. Localization Rule
\- Project internal language: English.
\- App user-facing text: localized to Latvian (values-lv).
\- Prefer English for all identifiers and logs.

\#\# Notes  
\- Context Window: keep analysis below 4K tokens per reply.  
\- File Naming Convention: lowercase\_with\_underscores for Python, PascalCase for Kotlin.  
\- For any uncertainty → ask clarification before editing.

\#\# Example Header Template  
\`\`\`kotlin  
// File: app/src/main/java/lv/mariozo/homeogo/ui/ElzaScreen.kt  
// Created: 09.Oct.2025 20:41    
// ver. 1.1  
// Purpose: Elza voice interaction screen (Compose UI)
// Comments:  
// \- Removed invalid import that caused a build error.

