<html>
	<body style="background-color: #e8edf1; padding: 0; margin: 0;">
		<table width="100%" height="400" cellpadding="0" cellspacing="0" style="margin:0; padding:0">
			<tr height="20"><td colspan="3"></td></tr>
			<tr>
				<td width="10%"></td>
				<td width="80%" valign="top" >
					<table style="background-color: #ffffff;" width="100%" cellpadding="0" cellspacing="0">
						<tr height="15"><td colspan="3"></td></tr>
						<tr>
							<td width="15"></td>
							<td valign="center"style="font-size: 16px;font-family: 'Open Sans', Helvetica, sans-serif; font-weight: bold; ">A task was created for you!</td>
							<td width="15"></td>
						</tr>
						<tr height="15"><td colspan="3"></td></tr>
						<tr height="1" style="background-color: #e8edf1;"><td colspan="3"></td></tr>
						<tr colspan="3" height="25"><td></td></tr>
						<tr>
							<td width="15"></td>
							<td valign="top" style="font-size: 14px;font-family: 'Open Sans', Helvetica, sans-serif;">
								<#if taskName?has_content>The task '${taskName}'<#else>A nameless task</#if> was created in which you are a candidate. <br />You can claim the task from your Alfresco Process Services queued tasks.
							</td>
							<td width="15"></td>
						</tr>
						<tr height="30"><td colspan="3"></td></tr>
						<tr>
							<td width="15"></td>
							<td valign="top">
								<table style="background-color: #ffffff;" width="100%" cellpadding="0" cellspacing="0">
									<tr>
										<td valign="middle" style="font-size: 15px;font-family: 'Open Sans', Helvetica, sans-serif; text-align: left; font-weight: bold;  color: #ffffff;">
											<a href="${taskDirectUrl}" title="Open the task" style="text-decoration: none; text-align: center;color: #ffffff; display: inline-block; width: 300px; padding: 10px 0px;background-color: #36a7c4; border-radius: 3px; -moz-border-radius: 3px; -webkit-border-radius: 3px;">Open the task</a>
										</td>
									</tr>
								</table>
							</td>
							<td width="15"></td>
						</tr>
						<tr height="15"><td colspan="3"></td></tr>
						<tr height="1" style="background-color: #e8edf1;"><td colspan="3"></td></tr>
						<tr height="15"><td colspan="3"></td></tr>
						<tr>
							<td width="15"></td>
							<td style="font-size: 11px;font-family: 'Open Sans', Helvetica, sans-serif;  color:#666666;">
								This email is sent to you by <a href="${homeUrl}" style="color: #666666;">Alfresco Process Services</a>. If you are not the intended recipient of this email, <a href="${homeUrl}" style="color: #666666;">please contacts us</a>.
							</td>
							<td width="15"></td>
						</tr>
						<tr height="25"><td colspan="3"></td></tr>
					</table>
				</td>
				<td width="10%"></td>
			</tr>
		</table>
	</body>
</html>