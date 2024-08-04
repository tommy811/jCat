// Groovy 语法提示服务
import { windows } from "codemirror/src/util/browser";

import CodeMirror, { commands } from 'codemirror'
import groovyBeautify from "groovy-beautify";

export class GroovySyntax {

    constructor(cm, consoleService, getSession) {
        if (!(getSession instanceof Function)) {
            throw new Error('getSession必须是函数')
        }
        this.cm = cm;// 编辑器
        this.getSession = getSession;
        this.consoleService = consoleService;
        // 添加快捷键
        // this.keyMap = adapterKeyMap({
        //     "Shift-Cmd-F": (cm) => this.doFormat(),
        //     "Shift-Alt-F": (cm) => this.doFormat(),
        //     "Ctrl-.": (cm) => this.showHint(),
        //     "Cmd-/": (cm) => this.doComment(),
        //     "Shift-Cmd-/": (cm) => this.doBlockComment(),
        // }, true);
        
        // document.addEventListener('keydown', function (event) {
        //     if (event.ctrlKey && event.shiftKey && (event.key == 'F' || event.key == 'K')) {
        //         console.log('触发f快捷键')
        //         event.preventDefault();
        //         // 你的保存操作代码
        //     }
        //     if (event.ctrlKey && event.key == '.') {
        //         console.log('触发.快捷键')
        //         event.preventDefault();
        //         // 你的保存操作代码
        //     }
        // });
        // this.cm.addKeyMap(this.keyMap, false);// 添加快捷键
        this.errors = []; //编译错误
    }
    doFormat() {
        // TODO 暂未实现
        let cursor = this.cm.getCursor();
        this.cm.setValue(groovyBeautify(this.cm.getValue()));
        this.cm.setCursor(cursor);
    }
    // 行注释
    doComment() {
        this.cm.toggleComment();
    }
    // 快注释
    doBlockComment() {
        this.cm.getDoc().listSelections().forEach(s => {
            if (!this.cm.uncomment(s.from(), s.to(), { fullLines: false })) {
                this.cm.blockComment(s.from(), s.to(), { fullLines: false });
            }
        });
    }

    // 通过.获取提示项
    showHintByInput() {
        this._showHintByInput();
        return CodeMirror.Pass;
    }
    async _showHintByInput() {
        //1. 判断提示条件
        // 2.编译
        let errors = await this.consoleService.analysis(this.getSession().id, this.cm.getValue());
        this.errors = errors;
        this.cm.performLint(); // 刷新提示
        if (this.errors.length > 0) {
            return;
        }
        // 3.基于光标位置获取提示项
        let cursor = this.cm.getCursor();
        let items = await this.consoleService.completion(this.getSession().id, [cursor.line + 1, cursor.ch], 200)
        if (items.length == 0)
            return;

        // 4.展示结果
        this.cm.showHint({ hint: (editor, options) => this.completionHints(editor, options, items) });
    }


    showHint() {
        let editor = this.cm;
        let cur = editor.getCursor();
        let curLine = editor.getLine(cur.line);
        let end = cur.ch, start = end;
        while (start && /[\w$]+/.test(curLine.charAt(start - 1))) --start;
        var keyword = start != end ? curLine.slice(start, end) : "";
        if (!keyword) return;
        //基于关键字查找提示项 并显示列表
        this.consoleService.completion(this.getSession().id, keyword, 200).then(items => {
            if (items.length == 0) return;
            this.cm.showHint({
                hint: (editor, options) => this.completionHints(editor, options, items),
            });
        })

    }
    // 获取完成提示项
    completionHints(editor, options, items) {
        var cur = editor.getCursor(), curLine = editor.getLine(cur.line);
        var end = cur.ch, start = end;
        while (start && /[\w$]+/.test(curLine.charAt(start - 1))) --start;
        var curWord = start != end ? curLine.slice(start, end) : "";

        let list = items
            .filter(i => {
                if (!curWord) {
                    i.showText = null;
                    return true;
                }
                let matchs = doFilter(curWord, i.filterText ? i.filterText : i.label, 'completionMark');
                if (matchs) {
                    i.showText = matchs.matchText;
                    i.score = matchs.score;
                    return true;
                }
                return false;;
            })
            .sort((a1, a2) => a2.score - a1.score)
            .map(i => {
                let render = (elm, self, data) => this.hintsRender(elm, i);
                let hint = (cm, self, data) => this.resolveHints(cm, i, curWord);
                return { render, hint };
            });

        return {
            list,
            from: { line: cur.line, ch: start },
            to: { line: cur.line, ch: end },
        };
    }
    // 样式渲染
    hintsRender(elm, item) {
        let html = "";
        let items = item.label.split(":");
        html = "<span class='completion  item primary'>" + (item.showText ? item.showText : items[0]) + "</span>";
        if (items.length > 1)
            html += "<span class='completion item secondary'>" + subStr(items[1], 50) + "</span>";
        if (items.length > 2)
            html += "<span class='completion item secondary right'>" + items[2] + "</span>";
        elm.innerHTML = html;
    }
    // 使用完成项
    resolveHints(cm, item, filterToken) {
        //.reverse()
        var cur = cm.getCursor();
        let newText = item.insertText;
        let oldValue = cm.getDoc().getValue();

        cm.getDoc().replaceRange(newText, { line: cur.line, ch: cur.ch - filterToken.length }, cur);
        // 插入tooltip 提示
        if (item.tipsText) {
            cm.getDoc().markText({ line: cur.line, ch: cur.ch - filterToken.length }, cm.getCursor(), { className: "mark-tooltip", message: item.tipsText });
            this.cm.performLint();// 刷新提示
        }
        if (item.insertImportText && !oldValue.includes(item.insertImportText)) {
            cm.getDoc().replaceRange(item.insertImportText + "\n", { line: 0, ch: 0 }, { line: 0, ch: 0 });
        }
        //
        if (newText.endsWith(")") || newText.endsWith("}")) {
            cur = cm.getCursor();
            cur.ch -= 1;
            this.cm.setCursor(cur);
        }

    }

}


//  windows 下所有Cmd替换成Ctrl
export function adapterKeyMap(keyMap) {
    if (windows) {
        for (const k of Object.getOwnPropertyNames(keyMap)) {
            if (k.indexOf("Cmd") > -1) {
                let fun = keyMap[k];
                delete (keyMap[k]);
                keyMap[k.replace("Cmd", "Ctrl")] = fun;
            }
        }
    }
    return keyMap;
}


function doFilter(queryText, itemText, mark, option) {
    queryText = queryText.toLocaleLowerCase();

    // 首字母必须匹配
    if (option && option.first && itemText.charAt(0).toLocaleLowerCase() != queryText.charAt(0)) {
        return false;
    }
    let j = 0;
    let matchText = "";
    let score = 50;
    // 
    for (let i = 0; i < itemText.length; i++) {
        if (itemText.charAt(i).toLocaleLowerCase() == queryText.charAt(j)) {
            j++;
            matchText +=
                "<span class='" + mark + "'>" + itemText.charAt(i) + "</span>";
            score += 20 - i * 2;
        } else {
            score--;
            matchText += itemText.charAt(i);
        }
    }
    if (j != queryText.length) {
        return false; //匹配失败
    }
    return { matchText, score };// 匹配成功结果
}

function subStr(text, maxLength) {
    return text.length > maxLength ? (text.substring(0, maxLength) + "...") : text;
}

export default {
    GroovySyntax,
    adapterKeyMap
}

function foundlints(text, options) {
    let errors = options.getLint(text);
    let errorLint = errors.map(l => {
        let r = {
            severity: l.level,
            from: CodeMirror.Pos(l.range[0] - 1, l.range[1] - 2),
            to: CodeMirror.Pos(l.range[2] - 1, l.range[3] - 1),
            message: l.message
        }
        return r;
    })

    let cm = options.getCm(); // 获取实例
    let tooltipLints = !cm ? [] : cm.getAllMarks().filter(m => m.className == 'mark-tooltip').map(t => {
        let p=t.find();
        let r = {
            severity: "tooltip",// 或者 warning
            from: p.from,
            to: p.to,
            message: t.message
        }
        return r;
    })

    return [...errorLint, ...tooltipLints]; // 合并数组返回
}
// 异常提示
CodeMirror.registerHelper("lint", "text/x-java", foundlints);