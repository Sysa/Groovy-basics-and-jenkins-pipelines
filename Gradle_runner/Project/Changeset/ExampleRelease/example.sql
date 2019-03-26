create table [example]
(
	[Id] int identity(1,1) not null,
	[Created] datetime not null,
	[Name] nvarchar(max) not null,
	constraint [PK_Examples] primary key (Id)
)