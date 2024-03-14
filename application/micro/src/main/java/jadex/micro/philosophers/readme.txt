This example shows different implementations for the well-known dining philosophers problem.

I) Thread based implementation
- uses threads for philosopher execution
- uses the sticks as synchronization objects with wait notify

II) Agent based implementation
- uses agents for philosopher and table execution
- uses the sticks as synchronization objects with future queues,
i.e. each getStick() call on table agent is enqueued if the
stick is not available
- table is realized as agent to protect the sticks against
concurrent calls and allow future return values. additionally
this allows for distributed execution.

The agent based uses higher level synchronization with futures.
I can be executed also in a distributed manner without code
changes.
