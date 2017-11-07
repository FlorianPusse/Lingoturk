SELECT fileName, listNumber, GraphCreation_number, GraphCreation_label, GraphCreation_text, GraphCreation_options, GraphCreation_descriptions, GraphCreation_optionList, assignmentId, hitId, workerId, origin, timestamp, partId, questionId, answer FROM (
	SELECT * FROM GraphCreationResults
	LEFT OUTER JOIN Questions USING (QuestionId)
	LEFT OUTER JOIN Groups USING (PartId)
) as tmp
WHERE LingoExpModelId = 681
ORDER BY partId, questionId, workerId