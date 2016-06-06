SELECT WorkerId, timestamp, partNumber, number AS chunkNumber, picFileName, result
FROM
	(
	SELECT * FROM
		(SELECT *, PictureNamingResults.answer AS result FROM Questions
		JOIN PictureNamingResults
		ON QuestionId = chunkId) AS chunkIdSubquery
	JOIN
		(SELECT number AS partNumber,partId FROM Groups) AS partIdSubquery
	USING (PartId)) AS secondSubquery
JOIN
	(SELECT id,file_Name AS picFileName FROM PictureNaming) AS thirdQuery
ON pictureId = thirdQuery.Id;