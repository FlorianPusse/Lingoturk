SELECT fileName, listNumber, StoryCompletion_itemId, StoryCompletion_storyType, StoryCompletion_story, assignmentId, hitId, workerId, origin, timestamp, partId, questionId, itemId, answer FROM (
	SELECT * FROM StoryCompletionResults
	LEFT OUTER JOIN Questions USING (QuestionId)
	LEFT OUTER JOIN Groups USING (PartId)
) as tmp
WHERE LingoExpModelId = 121
ORDER BY partId, questionId, workerId