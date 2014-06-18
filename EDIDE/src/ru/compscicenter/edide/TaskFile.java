package ru.compscicenter.edide;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.markup.RangeHighlighter;

import java.util.ArrayList;

/**
 * User: lia
 * Date: 30.05.14
 * Time: 20:30
 */

public class TaskFile {
    private final String name;
    private final ArrayList<TaskWindow> taskWindows;
    private int myLastLength;

    public TaskFile(String name, int taskWindowsNum) {
        this.name = name;
        taskWindows = new ArrayList<TaskWindow>(taskWindowsNum);
    }

    public int getLastLength() {
        return myLastLength;
    }

    public void addTaskWindow(TaskWindow taskWindow) {
        taskWindows.add(taskWindow);
    }

    public String getName() {
        return name;
    }


    public TaskWindow getTaskWindow(LogicalPosition pos) {
        int line = pos.line + 1;
        int offset = pos.column;
        int i = 0;
        while (i < taskWindows.size() && (taskWindows.get(i).getLine() < line ||
                (taskWindows.get(i).getLine() == line && taskWindows.get(i).getStartOffset() < offset))) {
            i++;
        }
        if (i == 0) {
            return null;
        }
        return taskWindows.get(i - 1);
    }

    public int getTaskWindowNum() {
        return taskWindows.size();
    }

    public TaskWindow getTaskWindowByIndex(int index) {
        return taskWindows.get(index);
    }

    public void drawFirstUnresolved(final Editor editor) {
        myLastLength = editor.getDocument().getTextLength();
        //TODO: maybe it's worth to find window with min startOffset
        for (TaskWindow tw : taskWindows) {
            if (!tw.getResolveStatus()) {
                tw.draw(editor);
                return;
            }
        }
    }

    public int resolveCurrentHighlighter(final Editor editor, final LogicalPosition pos) {
        RangeHighlighter[] rm = editor.getMarkupModel().getAllHighlighters();
        RangeHighlighter tmp = rm[0];
        if (rm.length > 1) {
            editor.getMarkupModel().removeAllHighlighters();
            //Log.print("Too many range markers :(");
            //Log.flush();
            //throw new IllegalArgumentException("too many range markers");
        }
        for (TaskWindow tw : taskWindows) {
            if (tw.getRangeHighlighter() != null) {
                int startOffset = tw.getRangeHighlighter().getStartOffset();
                int endOffset = tw.getRangeHighlighter().getEndOffset();
                if (startOffset == rm[0].getStartOffset() && endOffset == rm[0].getEndOffset()) {

                    tw.setResolved();
                    editor.getMarkupModel().removeAllHighlighters();
                    return startOffset;
                }
            }
        }
        return -1;
    }

    public void incrementAllTaskWindows(Editor editor, int lastResolvedStartOffset, int delta) {
        if (lastResolvedStartOffset == -1) {
            return;
        }
        for (TaskWindow tw : taskWindows) {
            int startOffset = editor.getDocument().getLineStartOffset(tw.getLine()) + tw.getStartOffset();
            if (startOffset > lastResolvedStartOffset) {
                tw.incrementStartOffset(delta);
            }
        }
    }
}
