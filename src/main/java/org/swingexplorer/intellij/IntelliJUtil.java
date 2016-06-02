package org.swingexplorer.intellij;

import com.intellij.ide.impl.ProjectUtil;
import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;

/**
 * Allgemeine Interaktionen mit IntelliJ m�glich machen
 *
 * @author W.Glanzer, 25.09.2015
 */
public class IntelliJUtil
{

  static
  {
    Notifications.Bus.register(IStaticIDs.NOTIFICATION_ID, NotificationDisplayType.NONE);
    Notifications.Bus.register(IStaticIDs.NOTIFICATION_ID_WINDOW, NotificationDisplayType.TOOL_WINDOW);
  }

  /**
   * Zeug im EventLog ausgeben
   *
   * @param pType    Typ der Notification
   * @param pMessage Nachricht, die ausgegeben werden soll
   */
  public static void notifiy(NotificationType pType, String pMessage)
  {
    Notifications.Bus.notify(new Notification(IStaticIDs.NOTIFICATION_ID, "", "SwingExplorer: " + pMessage, pType));
  }

  /**
   * Zeug im EventLog und als Balloon ausgeben
   *
   * @param pType    Typ der Notification
   * @param pMessage Nachricht, die ausgegeben werden soll
   */
  public static void notifiyBalloon(NotificationType pType, String pMessage)
  {
    Notifications.Bus.notify(new Notification(IStaticIDs.NOTIFICATION_ID_WINDOW, "", pMessage, pType));
  }

  /**
   * Navigiert zu einer bestimmten Zeile einer Klasse eines Projekts
   *
   * @param pProject    Projekt, zu dem navigiert werden soll
   * @param pClass      Klasse innerhalb des Projekts
   * @param pLineNumber Zeilennummer, zu der gesprungen werden soll
   */
  public static void navigate(final Project pProject, final String pClass, final int pLineNumber)
  {
    ApplicationManager.getApplication().invokeLater(new Runnable()
    {
      @Override
      public void run()
      {
        ApplicationManager.getApplication().runReadAction(new Runnable()
        {
          @Override
          public void run()
          {
            if (!pProject.isInitialized())
              return;

            final GlobalSearchScope searchScope = GlobalSearchScope.allScope(pProject);
            PsiClass psiClass = JavaPsiFacade.getInstance(pProject).findClass(pClass, searchScope);

            if (psiClass != null)
            {
              FileEditorProviderManager editorProviderManager = FileEditorProviderManager.getInstance();
              VirtualFile virtualFile = psiClass.getContainingFile().getVirtualFile();

              if (virtualFile == null || editorProviderManager.getProviders(pProject, virtualFile).length == 0)
                return;
              OpenFileDescriptor descriptor = new OpenFileDescriptor(pProject, virtualFile);

              final Editor editor = FileEditorManager.getInstance(pProject).openTextEditor(descriptor, true);
              if (editor != null)
              {
                if (pLineNumber > editor.getDocument().getLineCount())
                  return;

                CaretModel caretModel = editor.getCaretModel();
                LogicalPosition pos = new LogicalPosition(pLineNumber - 1, 0);
                caretModel.moveToLogicalPosition(pos);

                ApplicationManager.getApplication().invokeLater(new Runnable()
                {
                  public void run()
                  {
                    editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
                  }
                });
              }

              ProjectUtil.focusProjectWindow(pProject, true);
            }
            else
              notifiyBalloon(NotificationType.ERROR, "Class not found: " + pClass);
          }
        });
      }
    });
  }
}
