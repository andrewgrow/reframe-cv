package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import reframecv.domain.models.resume.content.ResumeProject
import reframecv.domain.models.resume.content.ResumeSkillOrTool
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_add_project
import reframecv.shared.generated.resources.resume_add_tool
import reframecv.shared.generated.resources.resume_cancel
import reframecv.shared.generated.resources.resume_category
import reframecv.shared.generated.resources.resume_description
import reframecv.shared.generated.resources.resume_project_name
import reframecv.shared.generated.resources.resume_remove_project
import reframecv.shared.generated.resources.resume_remove_project_question
import reframecv.shared.generated.resources.resume_remove_project_title
import reframecv.shared.generated.resources.resume_remove_tool
import reframecv.shared.generated.resources.resume_remove_tool_question
import reframecv.shared.generated.resources.resume_remove_tool_title
import reframecv.shared.generated.resources.resume_tool_name
import reframecv.shared.generated.resources.resume_tools
import reframecv.ui.components.common.confirmation.ConfirmationDialogContent
import reframecv.ui.components.common.confirmation.ConfirmationDialogTone
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ResumeFormScope.ProjectEntries(
    projects: List<ResumeProject>,
    onChange: (List<ResumeProject>) -> Unit,
) {
    Entries(
        projects,
        onChange,
        ::ResumeProject,
        Res.string.resume_add_project,
        removeAction = { project, remove ->
            ConfirmedRemoveAction(
                ConfirmationDialogContent(
                    title = Res.string.resume_remove_project_title,
                    message = Res.string.resume_remove_project_question,
                    confirmLabel = Res.string.resume_remove_project,
                    cancelLabel = Res.string.resume_cancel,
                    subject = project.name.takeIf { it.isNotBlank() },
                    tone = ConfirmationDialogTone.Critical,
                ),
                remove,
            )
        },
    ) { project, update ->
        Field(project.name, Res.string.resume_project_name, { update(project.copy(name = it)) })
        Field(project.description, Res.string.resume_description, {
            update(project.copy(description = it))
        }, multiline = true)
        Column(
            Modifier.padding(top = ReframeTheme.tokens.spacing.small),
            verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
        ) {
            Heading(Res.string.resume_tools)
            ToolEntries(project.skillsOrTools) { update(project.copy(skillsOrTools = it)) }
        }
    }
}

@Composable
private fun ResumeFormScope.ToolEntries(
    tools: List<ResumeSkillOrTool>,
    onChange: (List<ResumeSkillOrTool>) -> Unit,
) {
    Entries(
        tools,
        onChange,
        ::ResumeSkillOrTool,
        Res.string.resume_add_tool,
        removeAction = { tool, remove ->
            ConfirmedRemoveAction(
                ConfirmationDialogContent(
                    title = Res.string.resume_remove_tool_title,
                    message = Res.string.resume_remove_tool_question,
                    confirmLabel = Res.string.resume_remove_tool,
                    cancelLabel = Res.string.resume_cancel,
                    subject = tool.name.takeIf { it.isNotBlank() },
                    tone = ConfirmationDialogTone.Critical,
                ),
                remove,
            )
        },
    ) { tool, edit ->
        Field(tool.name, Res.string.resume_tool_name, { edit(tool.copy(name = it)) })
        Field(tool.category, Res.string.resume_category, { edit(tool.copy(category = it)) })
    }
}
