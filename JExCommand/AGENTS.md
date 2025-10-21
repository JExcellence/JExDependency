# JExCommand Agent Notes

> **Note:** Read the root `AGENTS.md` for repository-wide workflow, commit, and testing guidelines before following the module-specific notes below.

## Documentation standards
- Keep documentation in this module concise and organized using Markdown headings.
- When describing command workflows, reference required annotations, classes, and configuration naming conventions explicitly.
- Explain permission handling expectations including references to helper utilities and error contracts.

## Writing style
- Use sentence case for section titles and bold text for important class names or annotations.
- Provide bullet lists for step-by-step workflows or requirements.

## CommandFactory workflow
The reflection-based **CommandFactory** bootstraps commands using the following process:
- Only classes annotated with **@Command** are discovered and registered.
- Every command must have a paired section class named **`CommandNameSection`** that extends the shared section base type.
- The section class is created first and passed into the command constructor.
- Command constructors must accept the section instance as their first parameter, followed by the plugin or context objects they depend on (for example, the Paper plugin, localization manager, or scheduler).
- The factory uses those constructor signatures to instantiate command handlers before delegating to Paper's registration APIs.

## Configuration conventions
- Configuration sections must use the **`CommandNameSection`** naming scheme to match their command handler.
- YAML files that ship defaults must follow the **`CommandName.yml`** convention inside the module's resources.
- The command updater scans the sections and YAML files to sync registrations, ensuring any new command annotated with **@Command** is automatically registered when its section and configuration follow these naming rules.

## Permission handling
- Commands should leverage helper methods in **PermissionsSection** to evaluate whether senders hold the required permissions.
- When a non-player sender lacks permission or triggers a player-only command, throw a **CommandError** populated with the appropriate message for the console or automation context.
- The **CommandError** contract ensures messaging remains consistent and prevents the command pipeline from swallowing permission-related failures.
