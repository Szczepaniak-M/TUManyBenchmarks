import {KeyCode, KeyMod} from "monaco-editor";

export const environment = {
  production: true,
  apiUrl: "https://cqnkii3873.execute-api.us-east-1.amazonaws.com//api",
  repositoryUrl: "https://github.com/Szczepaniak-M/TUManyBenchmarks-benchmarks",
  addCommandMonacoEditor: (editor: any, emitter: any) => editor.addCommand(KeyMod.CtrlCmd | KeyCode.Enter, () => emitter.emit(true)),
};
