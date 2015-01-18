package com.nmote.mcf.sieve.commands;

import org.apache.jsieve.Arguments;
import org.apache.jsieve.Block;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.commands.AbstractActionCommand;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.Action;
import org.apache.jsieve.mail.MailAdapter;

public class Route extends AbstractActionCommand {

	protected Object executeBasic(MailAdapter mail, Arguments arguments, Block block, SieveContext context)
			throws SieveException {
		final String destination = ((StringListArgument) arguments.getArgumentList().get(0)).getList().get(0);

		// Only one route per destination allowed, others should be
		// discarded
		boolean isDuplicate = false;
		for (Action action : mail.getActions()) {
			isDuplicate = (action instanceof ActionRoute)
					&& (((ActionRoute) action).getDestination().equals(destination));
			if (isDuplicate) {
				break;
			}
		}

		if (!isDuplicate) {
			mail.addAction(new ActionRoute(destination));
		}

		return null;
	}

	protected void validateArguments(Arguments arguments, SieveContext context) throws SieveException {
		validateSingleStringArguments(arguments, context);
	}

}